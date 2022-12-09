package io.joern.odb2
import io.joern.odb2.StorageManifest._
import io.joern.odb2.jsonSerialization.readStorageContainer

import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable

object StorageTyp {
  val Bool   = "bool"
  val Byte   = "byte"
  val Short  = "short"
  val Int    = "int"
  val Long   = "long"
  val Ref    = "ref"
  val String = "string"
  val Double = "double"
  val Float  = "float"
}
object StorageManifest {
  class GraphStorage(
    var nodes: Array[NodeItem],
    var edges: Array[EdgeItem],
    var properties: Array[PropertyItem],
    val stringpool: StorageContainer[String]
  )

  class NodeItem(val nodeLabel: String, val nnodes: Int, var deletions: StorageContainer[Int])

  class EdgeItem(
    val nodeLabel: String,
    val edgeLabel: String,
    val inout: Int,
    var qty: StorageContainer[Int],
    var neighbors: StorageContainer[Long],
    var property: StorageContainer[_]
  )

  class PropertyItem(val nodeLabel: String, val propertyLabel: String, var qty: StorageContainer[Int], var property: StorageContainer[_])

  trait StorageContainer[T] {
    def typ: String
    def contents: Array[T]
  }
  class InlineStorage[T](val typ: String, val contents: Array[T]) extends StorageContainer[T]
}

class StorageConfig

object jsonSerialization {
  def readStorageContainer(serialized: ujson.Value): StorageContainer[_] = {
    if (serialized.isNull) return null
    val typ = serialized.obj("type").str
    if (serialized("encoding") != "inline") ???
    val values = serialized("values").arr
    typ match {
      case StorageTyp.Bool   => new InlineStorage[Boolean](typ, values.map { _.bool }.toArray)
      case StorageTyp.Byte   => new InlineStorage[Byte](typ, values.map { _.num.toByte }.toArray)
      case StorageTyp.Short  => new InlineStorage[Short](typ, values.map { _.num.toShort }.toArray)
      case StorageTyp.Int    => new InlineStorage[Int](typ, values.map { _.num.toInt }.toArray)
      case StorageTyp.Long   => new InlineStorage[Long](typ, values.map { _.num.toLong }.toArray)
      case StorageTyp.Float  => new InlineStorage[Float](typ, values.map { _.num.toFloat }.toArray)
      case StorageTyp.Double => new InlineStorage[Double](typ, values.map { _.num }.toArray)
      case StorageTyp.String => new InlineStorage[String](typ, values.map { _.str }.toArray)
      case StorageTyp.Ref    => new InlineStorage[Long](typ, values.map { _.num.toLong }.toArray)
    }
  }

}
object Serialization {

  def readContainer()

  def read(serialized: ujson.Obj): GraphStorage = {
    val version = serialized.obj.get("version")

    val nodes = serialized("nodes").arr.toArray.map { nodeItemJ =>
      val label  = nodeItemJ("nodelabel").str
      val nnodes = nodeItemJ("nnodes").num.toInt
      val del    = readStorageContainer(nodeItemJ("deletions")).asInstanceOf[StorageContainer[Int]]
      new NodeItem(label, nnodes, del)
    }

    val edgeItems = serialized("edges").arr.toArray.map { edgeItemJ =>
      val nodelabel = edgeItemJ("nodelabel").str
      val edgelabel = edgeItemJ("edgelabel").str
      val inout     = edgeItemJ("inout").num.toInt
      val qty       = readStorageContainer(edgeItemJ("qty")).asInstanceOf[StorageContainer[Int]]
      val neighbors = readStorageContainer(edgeItemJ("neighbors")).asInstanceOf[StorageContainer[Long]]
      val property  = readStorageContainer(edgeItemJ("property"))
      new EdgeItem(nodelabel, edgelabel, inout, qty = qty, neighbors = neighbors, property = property)

    }

    null
  }

  def encodeRefs(nodes: Array[GNode], config: StorageConfig): StorageContainer[Long] = {
    if (nodes == null) null
    else {
      val len = nodes.length
      val res = new Array[Long](len)
      for (idx <- Range(0, len)) {
        res(idx) = nodes(idx) match {
          case null        => 0x0000ffffffffffffL // -1 in 48 bit
          case node: GNode => (node.nodeKind.toLong << 32) + node.seq()
        }
      }
      new InlineStorage[Long](StorageTyp.Ref, res)
    }
  }

  def decodeRefs(nodes: Array[Array[GNode]], refs: StorageContainer[Long]): Array[GNode] = {
    assert(refs.typ == StorageTyp.Ref)
    val refLongs = refs.contents
    val res      = new Array[GNode](refLongs.length)
    for (idx <- Range(0, res.length)) {
      val kind = (refLongs(idx) >> 32).toShort
      val seq  = refLongs(idx).toInt
      res(idx) = if (kind >= 0) nodes(kind)(seq) else null
    }
    res
  }

  def encodeQty(nnodes: Int, qty: Array[Int], config: StorageConfig): StorageContainer[Int] = {
    val intermediate = new Array[Int](nnodes + 1)
    for (idx <- Range(0, scala.math.min(intermediate.length - 1, qty.length - 1))) {
      intermediate(idx) = qty(idx + 1) - qty(idx)
    }
    new InlineStorage[Int](StorageTyp.Int, intermediate)
  }

  def decodeQty(lens: StorageContainer[Int]): Array[Int] = {
    val res     = lens.contents.clone()
    var idx     = 0
    var counter = 0
    while (idx < res.length) {
      val tmp = res(idx)
      res(idx) = counter
      counter += tmp
      idx += 1
    }
    res
  }

  def encodeAny(item: Any, config: StorageConfig): StorageContainer[_] = {
    item match {
      case default: DefaultValue  => null
      case null                   => null
      case bools: Array[Boolean]  => new InlineStorage(StorageTyp.Bool, bools)
      case bytes: Array[Byte]     => new InlineStorage(StorageTyp.Byte, bytes)
      case shorts: Array[Short]   => new InlineStorage(StorageTyp.Short, shorts)
      case ints: Array[Int]       => new InlineStorage(StorageTyp.Int, ints)
      case longs: Array[Long]     => new InlineStorage(StorageTyp.Long, longs)
      case floats: Array[Float]   => new InlineStorage(StorageTyp.Float, floats)
      case doubles: Array[Double] => new InlineStorage(StorageTyp.Double, doubles)
      case refs: Array[GNode]     => encodeRefs(refs, config)
      case strings: Array[String] => new InlineStorage(StorageTyp.String, strings)
    }
  }

  def decodeAny(nodes: Array[Array[GNode]], item: StorageContainer[_]): AnyRef = {
    if (item == null) return null
    item.typ match {
      case StorageTyp.Ref    => decodeRefs(nodes, item.asInstanceOf[StorageContainer[Long]])
      case StorageTyp.Bool   => item.contents.asInstanceOf[Array[Boolean]]
      case StorageTyp.Byte   => item.contents.asInstanceOf[Array[Byte]]
      case StorageTyp.Short  => item.contents.asInstanceOf[Array[Short]]
      case StorageTyp.Int    => item.contents.asInstanceOf[Array[Int]]
      case StorageTyp.Long   => item.contents.asInstanceOf[Array[Long]]
      case StorageTyp.Float  => item.contents.asInstanceOf[Array[Float]]
      case StorageTyp.Double => item.contents.asInstanceOf[Array[Double]]
      case StorageTyp.String => item.contents.asInstanceOf[Array[String]]
    }
  }

  def serialize(g: Graph, filename: String, config: StorageConfig): StorageManifest.GraphStorage = {
    val nodes      = mutable.ArrayBuffer.empty[NodeItem]
    val edges      = mutable.ArrayBuffer.empty[EdgeItem]
    val properties = mutable.ArrayBuffer.empty[PropertyItem]
    for (nodeKind <- Range(0, g.schema.getNumberOfNodeKinds)) {
      val nodeLabel = g.schema.getNodeLabel(nodeKind)
      val deletions = g
        ._nodes(nodeKind)
        .iterator
        .collect {
          case deleted: GNode if AccessHelpers.isDeleted(deleted) => deleted.seq()
        }
        .toArray
      val size = g._nodes(nodeKind).size
      nodes.addOne(new StorageManifest.NodeItem(nodeLabel, size, new InlineStorage[Int](StorageTyp.Int, deletions)))
    }
    for (
      nodeKind <- Range(0, g.schema.getNumberOfNodeKinds);
      edgeKind <- Range(0, g.schema.getNumberOfEdgeKinds);
      inout    <- Range(0, 2)
    ) {
      val pos = g.schema.neighborOffsetArrayIndex(nodeKind, inout, edgeKind)
      if (g._neighbors(pos) != null) {
        val nodeLabel = g.schema.getNodeLabel(nodeKind)
        val edgeLabel = g.schema.getEdgeLabel(nodeKind, edgeKind)
        val edgeItem  = new StorageManifest.EdgeItem(nodeLabel, edgeLabel, inout, null, null, null)
        edges.addOne(edgeItem)
        edgeItem.qty = encodeQty(g._nodes(nodeKind).length, g._neighbors(pos).asInstanceOf[Array[Int]], config)
        edgeItem.neighbors = encodeRefs(g._neighbors(pos + 1).asInstanceOf[Array[GNode]], config)
        edgeItem.property = encodeAny(g._neighbors(pos + 2), config)
      }
    }
    for (
      nodeKind     <- Range(0, g.schema.getNumberOfNodeKinds);
      propertyKind <- Range(0, g.schema.getNumberOfProperties)
    ) {
      val pos = g.schema.propertyOffsetArrayIndex(nodeKind, propertyKind)
      if (g._properties(pos) != null) {
        val nodeLabel     = g.schema.getNodeLabel(nodeKind)
        val propertyLabel = g.schema.getPropertyLabel(nodeKind, propertyKind)
        val propertyItem  = new StorageManifest.PropertyItem(nodeLabel, propertyLabel, null, null)
        properties.addOne(propertyItem)
        propertyItem.qty = encodeQty(g._nodes(nodeKind).length, g._properties(pos).asInstanceOf[Array[Int]], config)
        propertyItem.property = encodeAny(g._properties(pos + 1), config)
      }
    }
    new GraphStorage(nodes.toArray, edges.toArray, properties.toArray, null)
  }

  def deserialize(g: Graph, manifest: GraphStorage): Unit = {
    val nodekinds = mutable.HashMap[String, Short]()
    for (nodeKind <- Range(0, g.schema.getNumberOfNodeKinds)) nodekinds(g.schema.getNodeLabel(nodeKind)) = nodeKind.toShort
    val kindRemapper = Array.fill(manifest.nodes.size)(-1.toShort)
    val nodeRemapper = new Array[Array[GNode]](manifest.nodes.length)
    for ((nodeItem, idx) <- manifest.nodes.zipWithIndex) {
      val nodeKind = nodekinds.get(nodeItem.nodeLabel)
      if (nodeKind.isDefined) {
        kindRemapper(idx) = nodeKind.get
        val nodes = new Array[GNode](nodeItem.nnodes)
        for (seq <- Range(0, nodes.length)) nodes(seq) = g.schema.makeNode(g, nodeKind.get, seq)
        g._nodes(nodeKind.get) = nodes
        nodeRemapper(idx) = nodes
        for (del <- nodeItem.deletions.contents) {
          AccessHelpers.markDeleted(nodes(del))
        }
      }
    }

    val edgeKinds = mutable.HashMap[(String, String), Short]()
    for (
      nodeKind <- Range(0, g.schema.getNumberOfNodeKinds);
      edgeKind <- Range(0, g.schema.getNumberOfEdgeKinds)
    ) {
      val nodeLabel = g.schema.getNodeLabel(nodeKind)
      val edgeLabel = g.schema.getEdgeLabel(nodeKind, edgeKind)
      if (edgeLabel != null) {
        edgeKinds((nodeLabel, edgeLabel)) = edgeKind.toShort
      }
    }

    for (edgeItem <- manifest.edges) {
      val nodeKind  = nodekinds.get(edgeItem.nodeLabel)
      val edgeKind  = edgeKinds.get(edgeItem.nodeLabel, edgeItem.edgeLabel)
      val direction = edgeItem.inout
      if (nodeKind.isDefined && edgeKind.isDefined) {
        val pos = g.schema.neighborOffsetArrayIndex(nodeKind.get, direction, edgeKind.get)
        g._neighbors(pos) = decodeQty(edgeItem.qty)
        g._neighbors(pos + 1) = decodeRefs(nodeRemapper, edgeItem.neighbors)
        val property = decodeAny(nodeRemapper, edgeItem.property)
        if (property != null)
          g._neighbors(pos + 2) = property
      }
    }

    val propertykinds = mutable.HashMap[(String, String), Int]()
    for (
      nodeKind     <- Range(0, g.schema.getNumberOfNodeKinds);
      propertyKind <- Range(0, g.schema.getNumberOfProperties)
    ) {
      val nodeLabel     = g.schema.getNodeLabel(nodeKind)
      val propertyLabel = g.schema.getPropertyLabel(nodeKind, propertyKind)
      if (propertyLabel != null) {
        propertykinds((nodeLabel, propertyLabel)) = propertyKind
      }
    }

    for (property <- manifest.properties) {
      val nodeKind     = nodekinds.get(property.nodeLabel)
      val propertyKind = propertykinds.get((property.nodeLabel, property.propertyLabel))
      if (nodeKind.isDefined && propertyKind.isDefined) {
        val pos = g.schema.propertyOffsetArrayIndex(nodeKind.get, propertyKind.get)
        g._properties(pos) = decodeQty(property.qty)
        g._properties(pos + 1) = decodeAny(nodeRemapper, property.property)
      }
    }

  }

}
class GraphSerializer {}
