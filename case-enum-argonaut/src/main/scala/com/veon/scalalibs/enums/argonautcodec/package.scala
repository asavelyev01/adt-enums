package com.veon.scalalibs.enums

import argonaut._
import Argonaut._

package object argonautcodec {
  implicit def enumCodec[E: CaseEnum]: CodecJson[E] =
    CodecJson[E](
      e => EncodeJson.of[String].apply(s"$e"),
      c =>
        for {
          name <- c.as[String]
          result <- CaseEnum[E].fromString(name) match {
            case Some(value) => DecodeResult.ok(value)
            case None        => DecodeResult.fail(s"Enum value $name doesn't exist", c.history)
          }
        } yield result
    )

}
