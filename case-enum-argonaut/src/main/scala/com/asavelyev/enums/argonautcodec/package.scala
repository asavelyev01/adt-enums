package com.asavelyev.enums

import argonaut.Argonaut._
import argonaut._

package object argonautcodec {
  implicit def enumCodec[E: CaseEnum]: CodecJson[E] =
    CodecJson[E](
      e => EncodeJson.of[String].apply(s"$e"),
      c =>
        for {
          name <- c.as[String]
          result <- CaseEnum[E].fromString(name) match {
            case Some(value) => DecodeResult.ok(value)
            case None => DecodeResult.fail(s"Enum value $name doesn't exist", c.history)
          }
        } yield result
    )

}
