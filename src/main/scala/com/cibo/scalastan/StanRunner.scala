/*
 * Copyright (c) 2017 - 2018 CiBO Technologies - All Rights Reserved
 * You may use, distribute, and modify this code under the
 * terms of the BSD 3-Clause license.
 *
 * A copy of the license can be found on the root of this repository,
 * at https://github.com/cibotech/ScalaStan/blob/master/LICENSE,
 * or at https://opensource.org/licenses/BSD-3-Clause
 */

package com.cibo.scalastan

import java.io._
import java.lang.ProcessBuilder.Redirect
import java.nio.file.{Files, Paths}
import java.util.regex.Pattern

import scala.collection.JavaConverters._
import scala.io.Source

protected trait StanRunner[M <: CompiledModel] {
  def compile(ss: ScalaStan, model: ScalaStan#Model): CompiledModel
  def run(
    model: M,
    chains: Int,
    seed: Int,
    cache: Boolean,
    method: RunMethod.Method
  ): StanResults
}

protected object StanRunner {

  // Use CmdStan to compile and run the model.
  implicit object CmdStanRunner extends StanRunner[CmdStanCompiledModel] {

    private val modelExecutable: String = "model"
    private val stanFileName: String = s"$modelExecutable.stan"
    private lazy val CMDSTAN_HOME: Option[String] = sys.env.get("CMDSTAN_HOME")

    // Find a file in the path.
    // Returns the full path to the file.
    private def findInPath(file: String): Option[String] = {
      System.getenv("PATH").split(Pattern.quote(File.pathSeparator)).map { p =>
        Paths.get(p).resolve(file)
      }.find(Files.isExecutable).map(_.toFile.getCanonicalPath)
    }

    // Look up the absolute path to "stanc", if it's in PATH.
    private def findStancInPath: Option[String] = {
      Stream("stanc", "stanc.exe").view.map(findInPath).collectFirst {
        case Some(p) => p
      }
    }

    // The Stan home directory.
    private lazy val stanHome: String = {
      // First check if CMDSTAN_HOME is set.
      // If CMDSTAN_HOME is not set, we attempt to infer it by looking up stanc.
      CMDSTAN_HOME.orElse {
        findStancInPath.map(p => new File(p).getParentFile.getParentFile.getAbsolutePath)
      }.getOrElse {
        throw new IllegalStateException("could not locate Stan")
      }
    }

    // Location of the "stanc" program.
    private lazy val stanc: String = s"$stanHome/bin/stanc"

    // The make program to use.
    private lazy val make: String = {
      Stream("gmake", "make", "gmake.exe", "make.exe").map(findInPath).collectFirst {
        case Some(p) => p
      }.getOrElse {
        throw new IllegalStateException("could not locate make")
      }
    }

    private def runStanc(dir: File): Unit = {
      val pb = new ProcessBuilder(stanc, stanFileName).directory(dir).inheritIO()
      val rc = pb.start().waitFor()
      if (rc != 0) {
        throw new IllegalStateException(s"$stanc returned $rc")
      }
    }

    private def runMake(dir: File): Unit = {
      val target = s"$dir/$modelExecutable"
      val pb = new ProcessBuilder(make, target).directory(new File(stanHome)).inheritIO()
      val rc = pb.start().waitFor()
      if (rc != 0) {
        throw new IllegalStateException(s"$make returned $rc")
      }
    }

    private def readIterations(fileName: String): Vector[Map[String, String]] = {
      val reader = new BufferedReader(new FileReader(fileName))
      try {
        val lines = reader.lines.iterator.asScala.filterNot(_.startsWith("#")).toVector
        if (lines.nonEmpty) {
          val header: Seq[String] = lines.head.split(',')
          lines.tail.map { sample =>
            header.zip(sample.split(',')).toMap
          }
        } else {
          Vector.empty
        }
      } finally {
        reader.close()
      }
    }

    private var dataFileIndex: Int = 0

    private def getNextDataFileName: String = {
      synchronized {
        dataFileIndex += 1
        s"data$dataFileIndex.R"
      }
    }

    def compile(ss: ScalaStan, model: ScalaStan#Model): CmdStanCompiledModel = {
      println(s"found stan in $stanHome")

      // Generate the code.
      val dir = model.generate

      if (new File(s"${dir.getPath}/$modelExecutable").canExecute) {
        println("found existing executable")
      } else {
        runStanc(dir)
        runMake(dir)
      }

      CmdStanCompiledModel(dir, ss)
    }

    def run(
      model: CmdStanCompiledModel,
      chains: Int,
      seed: Int,
      cache: Boolean,
      method: RunMethod.Method
    ): StanResults = {
      // Emit the data file.
      val dataFileName = getNextDataFileName
      println(s"writing data to $dataFileName")
      val dataWriter = ShaWriter(new PrintWriter(new File(s"${model.dir}/$dataFileName")))
      model.emitData(dataWriter)
      dataWriter.close()
      val runHash = dataWriter.sha.update(method.toString).digest

      val baseSeed = if (seed < 0) (System.currentTimeMillis % Int.MaxValue).toInt else seed
      val tempOutput = File.createTempFile("scalastan-out", ".tmp")
      val tempError  = File.createTempFile("scalastan-err", ".tmp")
      val results = (0 until chains).par.flatMap { i =>
        val chainSeed = baseSeed + i
        val name = s"$runHash-$seed-$i.csv"
        val fileName = s"${model.dir}/$name"
        val cachedResults = if (cache && new File(fileName).exists) {
          Some(readIterations(fileName))
        } else None
        cachedResults match {
          case Some(r) if r.nonEmpty =>
            println(s"Found cached results: $name")
            Some(r)
          case _                     =>
            val command = Vector(
              s"./$modelExecutable",
              "data", s"file=$dataFileName",
              "output", s"file=$name",
              "random", s"seed=$chainSeed"
            ) ++ method.arguments
            println("Running " + command.mkString(" "))
            val pb = new ProcessBuilder(command: _*)
              .directory(model.dir)
              .redirectInput(Redirect.INHERIT)
              .redirectOutput(Redirect.appendTo(tempOutput))
              .redirectError(Redirect.appendTo(tempError))
            val rc = pb.start().waitFor()
            if (rc != 0) {
              println(s"ERROR: model returned $rc")
              None
            } else {
              Some(readIterations(fileName))
            }
        }
      }.seq.toVector

      val stanOutput = Source.fromFile(tempOutput).getLines.mkString("\n")
      tempOutput.delete()
      val stanErrorOutput = Source.fromFile(tempError).getLines.mkString("\n")
      tempError.delete()
      StanResults(results, model, stanOutput, stanErrorOutput)
    }
  }
}
