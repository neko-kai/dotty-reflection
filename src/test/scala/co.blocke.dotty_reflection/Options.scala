package co.blocke.dotty_reflection

import munit._
import model._
import PrimitiveType._
import java.util.Optional

class Options extends munit.FunSuite {

  test("Scala optional field") {
    val r = Reflector.reflectOn[NormalOption].asInstanceOf[StaticClassInfo]
    val result = r match {
      case StaticClassInfo(
        "co.blocke.dotty_reflection.NormalOption",
        _,
        List(
          ScalaFieldInfo(0,"a",ScalaOptionInfo("scala.Option",_,Scala_Int),_,_,None)
        ),
        Nil,
        _,
        flase
        ) => true
      case _ => false
    }
    assert(result)
  }

  test("Java optional field") {
    val r = Reflector.reflectOn[co.blocke.reflect.JavaOption1].asInstanceOf[StaticJavaClassInfo]
    val result = r match {
      case StaticJavaClassInfo(
        "co.blocke.reflect.JavaOption1",
        _,
        List(
          JavaFieldInfo(0, "fld", JavaOptionInfo("java.util.Optional", _, Scala_Int),_,_,_)
        ),
        Nil,
        _) => true
      case _ => false
    }
    assert(result)
  }

  test("Scala nested optional field") {
    val r = Reflector.reflectOn[NestedOption].asInstanceOf[StaticClassInfo]
    val result = r match {
      case StaticClassInfo(
        "co.blocke.dotty_reflection.NestedOption",
        _,
        List(
          ScalaFieldInfo(0,"a",ScalaOptionInfo("scala.Option",_,ScalaOptionInfo("scala.Option",_,Scala_Int)),_,_,None)
        ),
        Nil,
        _,
        flase
        ) => true
      case _ => false
    }
    assert(result)
  }

  test("Java nested optional field") {
    val r = Reflector.reflectOn[co.blocke.reflect.JavaOption2].asInstanceOf[StaticJavaClassInfo]
    val result = r match {
      case StaticJavaClassInfo(
        "co.blocke.reflect.JavaOption2",
        _,
        List(
          JavaFieldInfo(0, "fld", JavaOptionInfo("java.util.Optional", _, JavaOptionInfo("java.util.Optional", _, Scala_Int)),_,_,_)
        ),
        Nil,
        _) => true
      case _ => false
    }
    assert(result)
  }

  test("Scala optional parameterized field") {
    val r = Reflector.reflectOn[ParamOption[_]].asInstanceOf[StaticClassInfo]
    val result = r match {
      case StaticClassInfo(
        "co.blocke.dotty_reflection.ParamOption",
        _,
        List(
          ScalaFieldInfo(0,"a",ScalaOptionInfo("scala.Option",_,"T"),_,_,None)
        ),
        List("T"),
        _,
        flase
        ) => true
      case _ => false
    }
    assert(result)
  }

  test("Java optional parameterized field") {
    val r = Reflector.reflectOn[co.blocke.reflect.JavaOption3[_]].asInstanceOf[StaticJavaClassInfo]
    val result = r match {
      case StaticJavaClassInfo(
        "co.blocke.reflect.JavaOption3",
        _,
        List(
          JavaFieldInfo(0, "fld", JavaOptionInfo("java.util.Optional", _, "T"),_,_,_)
        ),
        List("T"),
        _) => true
      case _ => false
    }
    assert(result)
  }

  test("Option assignments in union type") {
    val r = Reflector.reflectOn[UnionHavingOption].asInstanceOf[StaticClassInfo]
    assert(
      r.constructWith[UnionHavingOption](List(None,Optional.empty())) == UnionHavingOption(None,Optional.empty())
    )
    assert(
      r.constructWith[UnionHavingOption](List(Some(3),Optional.of(3))) == UnionHavingOption(Some(3),Optional.of(3))
    )
  }

  test("Option of a union") {    
    val r = Reflector.reflectOn[OptionHavingUnion].asInstanceOf[StaticClassInfo]
    val result = r match {
      case StaticClassInfo(
        "co.blocke.dotty_reflection.OptionHavingUnion",
        _,
        List(
          ScalaFieldInfo(0,"a",ScalaOptionInfo("scala.Option",_,StaticUnionInfo("__union_type__",Nil,Scala_Boolean,Scala_String)),_,_,None)
        ),
        Nil,
        _,
        flase
        ) => true
      case _ => false
    }
    assert(result)
  }

  test("Option of a union assignment") {    
    val r = Reflector.reflectOn[OptionHavingUnion].asInstanceOf[StaticClassInfo]
    assert(
      r.constructWith[OptionHavingUnion](List(None)) == OptionHavingUnion(None)
    )
    assert(
      r.constructWith[OptionHavingUnion](List(Some(true))) == OptionHavingUnion(Some(true))
    )
    assert(
      r.constructWith[OptionHavingUnion](List(Some("wow"))) == OptionHavingUnion(Some("wow"))
    )
  }
}