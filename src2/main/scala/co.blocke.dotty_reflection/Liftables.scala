package co.blocke.dotty_reflection

import quoted._
import java.io._

given Liftable[TypeSymbol] {
  def toExpr(t: TypeSymbol) = '{ ${Expr(t.asInstanceOf[String])}.asInstanceOf[TypeSymbol] }
}

given Liftable[RType] {
  def toExpr(x: RType) =
    '{ deserialize(${Expr(serialize(x)) }).asInstanceOf[RType] }
}

given Liftable[TypeMemberInfo] {
  def toExpr(x: TypeMemberInfo) =
    '{ new TypeMemberInfo(${Expr(x.name)}, ${Expr(x.typeSymbol)}, ${ Expr(x.memberType) } ) }
}

given Liftable[SelfRefRType] {
  def toExpr(x: SelfRefRType) =
    '{ new SelfRefRType(${Expr(x.name)}, ${ Expr( x.params ) } ) }
}


// In order to cross the compiler->runtime bridge, we need to serialize some objects, e.g. traits.
// Then on the runtime side we deserialize them back into objects again.
inline def serialize(o: Object): Array[Byte] = 
  val baos = new ByteArrayOutputStream()
  val oos  = new ObjectOutputStream(baos)
  oos.writeObject(o)
  val ret = baos.toByteArray
  baos.close
  oos.close
  ret

inline def deserialize(b: Array[Byte]): Object = 
  val bais = new ByteArrayInputStream(b)
  val ois  = new ObjectInputStream(bais)
  val ret = ois.readObject()
  bais.close
  ois.close
  ret
