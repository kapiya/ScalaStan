package com.cibo.scalastan

import scala.annotation.implicitNotFound

sealed trait TypeCheck

@implicitNotFound("${T} not a array/vector/matrix type")
sealed class IsCompoundType[T <: StanType]

object IsCompoundType {
  implicit def isCompound[T <: StanCompoundType] = new IsCompoundType[T]
}

@implicitNotFound("${T} not an int/real type")
sealed class IsScalarType[T <: StanType]

object IsScalarType {
  implicit def IsScalarType[T <: StanScalarType] = new IsScalarType[T]
}

@implicitNotFound("multiplication not allowed for ${R} = ${A} * ${B}")
sealed class MultiplicationAllowed[R <: StanType, A <: StanType, B <: StanType] extends TypeCheck

object MultiplicationAllowed {
  implicit val riMultiplication = new MultiplicationAllowed[StanReal, StanReal, StanInt]
  implicit val irMultiplication = new MultiplicationAllowed[StanReal, StanInt, StanReal]
  implicit def scalarMultiplication[T <: StanScalarType] = new MultiplicationAllowed[T, T, T]
  implicit def scalarVectorMultiplication[S <: StanScalarType, V <: StanCompoundType] = new MultiplicationAllowed[V, S, V]
  implicit def vectorScalarMultiplication[S <: StanScalarType, V <: StanCompoundType] = new MultiplicationAllowed[V, V, S]
  implicit val matVecMultiplication = new MultiplicationAllowed[StanVector, StanMatrix, StanVector]
  implicit val rvMatMultiplication = new MultiplicationAllowed[StanRowVector, StanRowVector, StanMatrix]
  implicit val rvVecMultiplication = new MultiplicationAllowed[StanReal, StanRowVector, StanVector]
  implicit val vecRvMultiplication = new MultiplicationAllowed[StanMatrix, StanVector, StanRowVector]
}

@implicitNotFound("division not allowed for ${R} = ${A} * ${B}")
sealed class DivisionAllowed[R <: StanType, A <: StanType, B <: StanType] extends TypeCheck

object DivisionAllowed {
  implicit val riDivision = new DivisionAllowed[StanReal, StanReal, StanInt]
  implicit val irDivision = new DivisionAllowed[StanReal, StanInt, StanReal]
  implicit def scalarDivision[T <: StanScalarType] = new DivisionAllowed[T, T, T]
  implicit def vecScalarDivision[V <: StanVectorLike, T <: StanScalarType] = new DivisionAllowed[V, V, T]
  implicit def matScalarDivision[T <: StanScalarType] = new DivisionAllowed[StanMatrix, StanMatrix, T]
  implicit val vecMatDivision = new DivisionAllowed[StanRowVector, StanRowVector, StanMatrix]
  implicit val matMatDivision = new DivisionAllowed[StanMatrix, StanMatrix, StanMatrix]
}

@implicitNotFound("left division not allowed for ${R} = ${A} * ${B}")
sealed class LeftDivisionAllowed[R <: StanType, A <: StanType, B <: StanType] extends TypeCheck

object LeftDivisionAllowed {
  implicit val matVecDivision = new LeftDivisionAllowed[StanVector, StanMatrix, StanVector]
  implicit val matMatDivision = new LeftDivisionAllowed[StanMatrix, StanMatrix, StanMatrix]
}

@implicitNotFound("element-wise division not allowed for ${R} = ${A} :/ ${B}")
sealed class ElementWiseDivisionAllowed[R <: StanType, A <: StanType, B <: StanType] extends TypeCheck

object ElementWiseDivisionAllowed {
  implicit def sameTypeDivision[T <: StanType] = new ElementWiseDivisionAllowed[T, T, T]
  implicit def csDivision[C <: StanCompoundType, S <: StanScalarType] = new ElementWiseDivisionAllowed[C, C, S]
  implicit def scDivision[C <: StanCompoundType, S <: StanScalarType] = new ElementWiseDivisionAllowed[C, S, C]
}

@implicitNotFound("addition not allowed for ${R} = ${A} + ${B}")
sealed class AdditionAllowed[R <: StanType, A <: StanType, B <: StanType] extends TypeCheck

object AdditionAllowed {
  implicit val riAddition = new AdditionAllowed[StanReal, StanReal, StanInt]
  implicit val irAddition = new AdditionAllowed[StanReal, StanInt, StanReal]
  implicit def sameTypeAddition[T <: StanType] = new AdditionAllowed[T, T, T]
  implicit def scalarVectorAddtion[V <: StanCompoundType, S <: StanScalarType] = new AdditionAllowed[V, S, V]
  implicit def vectorScalarAddtion[V <: StanCompoundType, S <: StanScalarType] = new AdditionAllowed[V, V, S]
}

@implicitNotFound("modulus not allowed for ${T} (only int)")
sealed class ModulusAllowed[T <: StanType] extends TypeCheck

object ModulusAllowed {
  implicit def intModulus[T <: StanInt] = new ModulusAllowed[T]
}

@implicitNotFound("logical not allowed for type ${T}")
sealed class LogicalAllowed[T <: StanType] extends TypeCheck

object LogicalAllowed {
  implicit def intLogical[T <: StanInt] = new LogicalAllowed[T]
  implicit def realLogical[T <: StanReal] = new LogicalAllowed[T]
}

@implicitNotFound("distance not allowed between types ${A} and ${B}")
sealed class DistanceAllowed[A <: StanType, B <: StanType] extends TypeCheck

object DistanceAllowed {
  implicit val vvDistance = new DistanceAllowed[StanVector, StanVector]
  implicit val vrDistance = new DistanceAllowed[StanVector, StanRowVector]
  implicit val rvDistance = new DistanceAllowed[StanRowVector, StanVector]
  implicit val rrDistance = new DistanceAllowed[StanRowVector, StanRowVector]
}

@implicitNotFound("transpose not allowed for ${R} = ${T}.t")
sealed class TransposeAllowed[T <: StanType, R <: StanType] extends TypeCheck

object TransposeAllowed {
  implicit val matrixTranspose = new TransposeAllowed[StanMatrix, StanMatrix]
  implicit val vectorTranspose = new TransposeAllowed[StanRowVector, StanVector]
  implicit val rowVectorTranspose = new TransposeAllowed[StanVector, StanRowVector]
}

@implicitNotFound("function only allowed in a GeneratedQuantity")
sealed trait InGeneratedQuantityBlock extends TypeCheck

object InGeneratedQuantityBlock extends InGeneratedQuantityBlock

@implicitNotFound("continuous type required, got ${T}")
sealed class ContinuousType[T <: StanType] extends TypeCheck

object ContinuousType {
  implicit def hasRealElement[T <: StanType](implicit ev: T#ELEMENT_TYPE =:= StanReal) = new ContinuousType[T]

  // Because Stan auto-converts ints to reals, we allow bare ints to be treated as continuous.
  implicit val canConvertToReal = new ContinuousType[StanInt]
}

@implicitNotFound("discrete type required, got ${T}")
sealed class DiscreteType[T <: StanType] extends TypeCheck

object DiscreteType {
  implicit def hasIntElement[T <: StanType](implicit ev: T#ELEMENT_TYPE =:= StanInt) = new DiscreteType[T]
}

@implicitNotFound("${T} not a vector, row vector, or array")
sealed class IsVectorLikeOrArray[T <: StanType] extends TypeCheck

object IsVectorLikeOrArray {
  implicit val isVector = new IsVectorLikeOrArray[StanVector]
  implicit val isRowVector = new IsVectorLikeOrArray[StanRowVector]
  implicit def isArray[T <: StanType] = new IsVectorLikeOrArray[StanArray[T]]
}

@implicitNotFound("${T} not a vector or matrix")
sealed class IsVectorOrMatrix[T <: StanType] extends TypeCheck

object IsVectorOrMatrix {
  implicit val isVector = new IsVectorOrMatrix[StanVector]
  implicit val isMatrix = new IsVectorOrMatrix[StanMatrix]
}

@implicitNotFound("${T} not a row vector or matrix")
sealed class IsRowVectorOrMatrix[T <: StanType] extends TypeCheck

object IsRowVectorOrMatrix {
  implicit val isRowVector = new IsRowVectorOrMatrix[StanRowVector]
  implicit val isMatrix = new IsRowVectorOrMatrix[StanMatrix]
}

@implicitNotFound("${T} not a vector, row vector, or matrix")
sealed class IsVectorLikeOrMatrix[T <: StanType] extends TypeCheck

object IsVectorLikeOrMatrix {
  implicit val isVector = new IsVectorLikeOrMatrix[StanVector]
  implicit val isRowVector = new IsVectorLikeOrMatrix[StanRowVector]
  implicit val isMatrix = new IsVectorLikeOrMatrix[StanMatrix]
}

@implicitNotFound("toMatrix not supported for type ${T}")
sealed class ToMatrixAllowed[T <: StanType] extends TypeCheck

object ToMatrixAllowed {
  implicit val isVector = new ToMatrixAllowed[StanVector]
  implicit val isRowVector = new ToMatrixAllowed[StanRowVector]
  implicit val isMatrix = new ToMatrixAllowed[StanMatrix]
  implicit val isIntArrayArray = new ToMatrixAllowed[StanArray[StanArray[StanInt]]]
  implicit val isRealArrayArray = new ToMatrixAllowed[StanArray[StanArray[StanReal]]]
}

@implicitNotFound("toVector not supported for type ${T}")
sealed class ToVectorAllowed[T <: StanType] extends TypeCheck

object ToVectorAllowed {
  implicit val isVector = new ToVectorAllowed[StanVector]
  implicit val isRowVector = new ToVectorAllowed[StanRowVector]
  implicit val isMatrix = new ToVectorAllowed[StanMatrix]
  implicit val isIntArray= new ToVectorAllowed[StanArray[StanInt]]
  implicit val isRealArray= new ToVectorAllowed[StanArray[StanReal]]
}

@implicitNotFound("appendCol not allowed for ${R} = appendCol(${X}, ${Y})")
sealed class AppendColAllowed[X <: StanType, Y <: StanType, R <: StanType]

object AppendColAllowed {
  implicit val appendColMM = new AppendColAllowed[StanMatrix, StanMatrix, StanMatrix]
  implicit val appendColMV = new AppendColAllowed[StanMatrix, StanVector, StanMatrix]
  implicit val appendColVM = new AppendColAllowed[StanVector, StanMatrix, StanMatrix]
  implicit val appendColVV = new AppendColAllowed[StanVector, StanVector, StanMatrix]
  implicit val appendColRR = new AppendColAllowed[StanRowVector, StanRowVector, StanRowVector]
  implicit val appendColDR = new AppendColAllowed[StanReal, StanRowVector, StanRowVector]
  implicit val appendColRD = new AppendColAllowed[StanRowVector, StanReal, StanRowVector]
}

@implicitNotFound("appendRow not allowed for ${R} = appendCol(${X}, ${Y})")
sealed class AppendRowAllowed[X <: StanType, Y <: StanType, R <: StanType]

object AppendRowAllowed {
  implicit val appendRowMM = new AppendRowAllowed[StanMatrix, StanMatrix, StanMatrix]
  implicit val appendRowMR = new AppendRowAllowed[StanMatrix, StanRowVector, StanMatrix]
  implicit val appendRowRM = new AppendRowAllowed[StanRowVector, StanMatrix, StanMatrix]
  implicit val appendRowRR = new AppendRowAllowed[StanRowVector, StanRowVector, StanMatrix]
  implicit def appendRowVV = new AppendRowAllowed[StanVector, StanVector, StanVector]
  implicit val appendRowDV = new AppendRowAllowed[StanReal, StanVector, StanVector]
  implicit val appendRowVD = new AppendRowAllowed[StanVector, StanReal, StanVector]
}
