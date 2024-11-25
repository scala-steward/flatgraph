package testdomains.codepropertygraphminified
import flatgraph.{DiffGraphApplier, DiffGraphBuilder}
import flatgraph.help.DocSearchPackages
import flatgraph.help.Table.AvailableWidthProvider
import testdomains.codepropertygraphminified.language.*

object CpgMinified {
  val defaultDocSearchPackage = DocSearchPackages.default.withAdditionalPackage(getClass.getPackage.getName)

  @scala.annotation.implicitNotFound("""If you're using flatgraph purely without a schema and associated generated domain classes, you can
    |start with `given DocSearchPackages = DocSearchPackages.default`.
    |If you have generated domain classes, use `given DocSearchPackages = MyDomain.defaultDocSearchPackage`.
    |If you have additional custom extension steps that specify help texts via @Doc annotations, use `given DocSearchPackages = MyDomain.defaultDocSearchPackage.withAdditionalPackage("my.custom.package)"`
    |""".stripMargin)
  def help(implicit searchPackageNames: DocSearchPackages, availableWidthProvider: AvailableWidthProvider) =
    flatgraph.help.TraversalHelp(searchPackageNames).forTraversalSources(verbose = false)

  @scala.annotation.implicitNotFound("""If you're using flatgraph purely without a schema and associated generated domain classes, you can
    |start with `given DocSearchPackages = DocSearchPackages.default`.
    |If you have generated domain classes, use `given DocSearchPackages = MyDomain.defaultDocSearchPackage`.
    |If you have additional custom extension steps that specify help texts via @Doc annotations, use `given DocSearchPackages = MyDomain.defaultDocSearchPackage.withAdditionalPackage("my.custom.package)"`
    |""".stripMargin)
  def helpVerbose(implicit searchPackageNames: DocSearchPackages, availableWidthProvider: AvailableWidthProvider) =
    flatgraph.help.TraversalHelp(searchPackageNames).forTraversalSources(verbose = true)

  def empty: CpgMinified = new CpgMinified(new flatgraph.Graph(GraphSchema))

  def from(initialElements: DiffGraphBuilder => DiffGraphBuilder): CpgMinified = {
    val graph = new flatgraph.Graph(GraphSchema)
    DiffGraphApplier.applyDiff(graph, initialElements(new DiffGraphBuilder(GraphSchema)))
    new CpgMinified(graph)
  }

  /** Instantiate a new graph with storage. If the file already exists, this will deserialize the given file into memory. `Graph.close` will
    * serialise graph to that given file (and override whatever was there before), unless you specify `persistOnClose = false`.
    */
  def withStorage(storagePath: java.nio.file.Path, persistOnClose: Boolean = true): CpgMinified = {
    val graph = flatgraph.Graph.withStorage(GraphSchema, storagePath, persistOnClose)
    new CpgMinified(graph)
  }

  def newDiffGraphBuilder: DiffGraphBuilder = new DiffGraphBuilder(GraphSchema)
}

class CpgMinified(private val _graph: flatgraph.Graph = new flatgraph.Graph(GraphSchema)) extends AutoCloseable {
  def graph: flatgraph.Graph = _graph

  def help(implicit searchPackageNames: DocSearchPackages, availableWidthProvider: AvailableWidthProvider) =
    CpgMinified.help
  def helpVerbose(implicit searchPackageNames: DocSearchPackages, availableWidthProvider: AvailableWidthProvider) =
    CpgMinified.helpVerbose

  override def close(): Unit =
    _graph.close()

  override def toString(): String =
    String.format("CpgMinified[%s]", graph)
}

@flatgraph.help.TraversalSource
class CpgMinifiedNodeStarters(val wrappedCpgMinified: CpgMinified) {

  @flatgraph.help.Doc(info = "all nodes")
  def all: Iterator[nodes.StoredNode] = wrappedCpgMinified.graph.allNodes.asInstanceOf[Iterator[nodes.StoredNode]]

  def id(nodeId: Long): Iterator[nodes.StoredNode] =
    Option(wrappedCpgMinified.graph.node(nodeId)).iterator.asInstanceOf[Iterator[nodes.StoredNode]]

  def ids(nodeIds: Long*): Iterator[nodes.StoredNode] = nodeIds.iterator.flatMap(id)

  /** */
  @flatgraph.help.Doc(info = """""")
  def call: Iterator[nodes.Call] = wrappedCpgMinified.graph._nodes(0).asInstanceOf[Iterator[nodes.Call]]

  /** */
  @flatgraph.help.Doc(info = """""")
  def method: Iterator[nodes.Method] = wrappedCpgMinified.graph._nodes(1).asInstanceOf[Iterator[nodes.Method]]

  /** subtypes: CALL
    */
  @flatgraph.help.Doc(info = """""", longInfo = """subtypes: CALL""")
  def callRepr: Iterator[nodes.CallRepr] = Iterator(this.call).flatten

  /** subtypes: METHOD
    */
  @flatgraph.help.Doc(info = """""", longInfo = """subtypes: METHOD""")
  def declaration: Iterator[nodes.Declaration] = Iterator(this.method).flatten

}