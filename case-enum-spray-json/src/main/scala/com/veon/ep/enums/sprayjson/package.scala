package com.veon.ep.enums

import spray.json
import spray.json._

package object sprayjson {

  /**
    * Spray json formatter for simple case objects with one trait
    *
    * @example
    * <pre>
    *   implicit val userStatusJsonFormat = enumFormat[UserStatus](
    *     NonSubscriberWithAccount,
    *     NonSubscriberWithoutAccount,
    *     SubscriberWithAccount,
    *     SubscriberWithoutAccount
    *   )
    *
    *   trait UserStatus
    *   case object NonSubscriberWithAccount    extends UserStatus
    *   case object SubscriberWithAccount       extends UserStatus
    *   case object SubscriberWithoutAccount    extends UserStatus
    *   case object NonSubscriberWithoutAccount extends UserStatus
    *   </pre>
    *
    * @param values case objects that needs to be formated
    * @tparam E trait, that all values extend
    * @return JsonFormat[E]
    */
  def enumFormat[E](values: E*): JsonFormat[E] = new JsonFormat[E] {
    private[this] def name(obj: E) = s"$obj"

    override def write(obj: E) = JsString(name(obj))

    override def read(value: JsValue) = value match {
      case JsString(string) =>
        values
          .find(name(_) == string)
          .getOrElse {
            json.deserializationError(s"Unknown object name: $string")
          }
      case _ =>
        json.deserializationError(s"String expected, got: $value")
    }
  }

  implicit def enumFormat[E: CaseEnum]: JsonFormat[E] = enumFormat(CaseEnum[E].all.toSeq: _*)

  def enumFormatWithDefault[E: CaseEnum](defaultValue: E): JsonFormat[E] =
    enumFormatWithDefault(CaseEnum[E].all.toSeq: _*)(defaultValue)

  def enumFormatWithDefault[E](values: E*)(defaultReadValue: E): JsonFormat[E] = new JsonFormat[E] {
    private[this] def name(obj: E) = s"$obj"

    override def write(obj: E) = JsString(name(obj))

    override def read(value: JsValue) = value match {
      case JsString(string) =>
        values
          .find(name(_) == string)
          .getOrElse(defaultReadValue)
      case _ =>
        defaultReadValue
    }
  }
}
