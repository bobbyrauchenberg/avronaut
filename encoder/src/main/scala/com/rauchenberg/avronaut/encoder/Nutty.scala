package com.rauchenberg.avronaut.encoder

import com.rauchenberg.avronaut.common.Avro
import com.rauchenberg.avronaut.decoder.Decoder

sealed trait JSchema[A]

case object JNumT  extends JSchema[Int]
case object JBoolT extends JSchema[Boolean]
case object JStrT  extends JSchema[String]

object DecoderJ {

  def apply[A](schema: JSchema[A]): Decoder[A] =
    schema match {
      case JNumT  => Decoder.intDecoder
      case JBoolT => Decoder.booleanDecoder
      case JStrT  => Decoder.stringDecoder
    }

  // def serialize[A](schema: JSchema[A], value: A): Avro

}

object M extends App {

  DecoderJ.apply(JBoolT)

}
