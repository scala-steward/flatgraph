package io.shiftleft.codepropertygraph.generated.v2.nodes

import io.joern.odb2
import io.shiftleft.codepropertygraph.generated.v2.Language.*
import scala.collection.immutable.{IndexedSeq, ArraySeq}

trait ClosureBindingEMT extends AnyRef with HasClosureBindingIdEMT with HasClosureOriginalNameEMT with HasEvaluationStrategyEMT

trait ClosureBindingBase extends AbstractNode with StaticType[ClosureBindingEMT] {

  override def propertiesMap: java.util.Map[String, Any] = {
    import io.shiftleft.codepropertygraph.generated.v2.accessors.Lang.*
    val res = new java.util.HashMap[String, Any]()
    this.closureBindingId.foreach { p => res.put("CLOSURE_BINDING_ID", p) }
    this.closureOriginalName.foreach { p => res.put("CLOSURE_ORIGINAL_NAME", p) }
    res.put("EVALUATION_STRATEGY", this.evaluationStrategy)
    res
  }
}

object ClosureBinding {
  val Label = "CLOSURE_BINDING"
  object PropertyKinds {
    val ClosureBindingId    = io.shiftleft.codepropertygraph.generated.v2.PropertyKinds.CLOSURE_BINDING_ID
    val ClosureOriginalName = io.shiftleft.codepropertygraph.generated.v2.PropertyKinds.CLOSURE_ORIGINAL_NAME
    val EvaluationStrategy  = io.shiftleft.codepropertygraph.generated.v2.PropertyKinds.EVALUATION_STRATEGY
  }
  object PropertyDefaults {
    val EvaluationStrategy = "<empty>"
  }
}

class ClosureBinding(graph_4762: odb2.Graph, seq_4762: Int)
    extends StoredNode(graph_4762, 8.toShort, seq_4762)
    with ClosureBindingBase
    with StaticType[ClosureBindingEMT] {

  override def productElementName(n: Int): String =
    n match {
      case 0 => "closureBindingId"
      case 1 => "closureOriginalName"
      case 2 => "evaluationStrategy"
      case _ => ""
    }

  override def productElement(n: Int): Any =
    n match {
      case 0 => this.closureBindingId
      case 1 => this.closureOriginalName
      case 2 => this.evaluationStrategy
      case _ => null
    }

  override def productPrefix = "ClosureBinding"
  override def productArity  = 3

  override def canEqual(that: Any): Boolean = that != null && that.isInstanceOf[ClosureBinding]
}

object NewClosureBinding { def apply(): NewClosureBinding = new NewClosureBinding }
class NewClosureBinding extends NewNode(8.toShort) with ClosureBindingBase {
  type RelatedStored = ClosureBinding
  override def label: String                                = "CLOSURE_BINDING"
  var closureBindingId: Option[String]                      = None
  var closureOriginalName: Option[String]                   = None
  var evaluationStrategy: String                            = "<empty>": String
  def closureBindingId(value: Option[String]): this.type    = { this.closureBindingId = value; this }
  def closureBindingId(value: String): this.type            = { this.closureBindingId = Option(value); this }
  def closureOriginalName(value: Option[String]): this.type = { this.closureOriginalName = value; this }
  def closureOriginalName(value: String): this.type         = { this.closureOriginalName = Option(value); this }
  def evaluationStrategy(value: String): this.type          = { this.evaluationStrategy = value; this }
  override def flattenProperties(interface: odb2.BatchedUpdateInterface): Unit = {
    if (closureBindingId.nonEmpty) interface.insertProperty(this, 8, this.closureBindingId)
    if (closureOriginalName.nonEmpty) interface.insertProperty(this, 9, this.closureOriginalName)
    interface.insertProperty(this, 19, Iterator(this.evaluationStrategy))
  }

  override def productElementName(n: Int): String =
    n match {
      case 0 => "closureBindingId"
      case 1 => "closureOriginalName"
      case 2 => "evaluationStrategy"
      case _ => ""
    }

  override def productElement(n: Int): Any =
    n match {
      case 0 => this.closureBindingId
      case 1 => this.closureOriginalName
      case 2 => this.evaluationStrategy
      case _ => null
    }

  override def productPrefix                = "NewClosureBinding"
  override def productArity                 = 3
  override def canEqual(that: Any): Boolean = that != null && that.isInstanceOf[NewClosureBinding]
}