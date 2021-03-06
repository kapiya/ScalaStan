package com.cibo.scalastan

class TransformedDataSpec extends ScalaStanBaseSpec {
  describe("DataTransform") {
    it("should allow assignment to result") {
      new ScalaStan {
        val t = new TransformedData(real()) {
          result := 5.0
        }
        val model = new Model { local(real()) := t }
        checkCode(model, "transformed data { real t; { t = 5.0; } }")
      }
    }

    it("should not allow assignment to parameters") {
      """
      new ScalaStan {
        val p = parameter(real())
        new DataTransform(real()) {
          p := 5
        }
      }
      """ shouldNot compile
    }
  }
}
