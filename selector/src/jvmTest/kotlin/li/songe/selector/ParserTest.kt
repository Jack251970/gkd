package li.songe.selector

import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import li.songe.selector.parser.ParserSet
import li.songe.selector.util.filterIndexes
import org.junit.Test
import java.io.File


class ParserTest {

    @Test
    fun test_expression() {
        println(ParserSet.expressionParser("a>1&&b>1&&c>1||d>1", 0).data)
        println(Selector.parse("View[a>1&&b>1&&c>1||d>1&&x^=1] > Button[a>1||b*='zz'||c^=1]"))
        println(Selector.parse("[id=`com.byted.pangle:id/tt_splash_skip_btn`||(id=`com.hupu.games:id/tv_time`&&text*=`跳过`)]"))
    }

    @Test
    fun string_selector() {
        val text =
            "ImageView < @FrameLayout < LinearLayout < RelativeLayout <n LinearLayout < RelativeLayout + LinearLayout > RelativeLayout > TextView[text\$='广告']"
        println("trackIndex: " + Selector.parse(text).trackIndex)
    }

    @Test
    fun query_selector() {
        val projectCwd = File("../").absolutePath
        val text =
            "* > View[isClickable=true][childCount=1][textLen=0] > Image[isClickable=false][textLen=0]"
        val selector = Selector.parse(text)
        println("selector: $selector")

        val jsonString = File("$projectCwd/_assets/snapshot-1686629593092.json").readText()
        val json = Json {
            ignoreUnknownKeys = true
        }
        val nodes = json.decodeFromString<TestSnapshot>(jsonString).nodes

        nodes.forEach { node ->
            node.parent = nodes.getOrNull(node.pid)
            node.parent?.apply {
                children.add(node)
            }
        }
        val transform = Transform<TestNode>(getAttr = { node, name ->
            val value = node.attr[name] ?: return@Transform null
            if (value is JsonNull) return@Transform null
            value.intOrNull ?: value.booleanOrNull ?: value.content
        }, getName = { node -> node.attr["name"]?.content }, getChildren = { node ->
            node.children.asSequence()
        }, getParent = { node -> node.parent })
        val targets = transform.querySelectorAll(nodes.first(), selector).toList()
        println("target_size: " + targets.size)
        assertTrue(targets.size == 1)
        println("id: " + targets.first().id)

        val trackTargets = transform.querySelectorTrackAll(nodes.first(), selector).toList()
        println("trackTargets_size: " + trackTargets.size)
        assertTrue(trackTargets.size == 1)
        println(trackTargets.first().mapIndexed { index, testNode ->
            testNode.id to selector.tracks[index]
        })
    }

    @Test
    fun check_parser() {
        println(Selector.parse("View > Text"))
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private fun getTreeNode(name: String): TestNode {
        val jsonString = File("../_assets/$name").readText()
        val nodes = json.decodeFromString<TestSnapshot>(jsonString).nodes
        nodes.forEach { node ->
            node.parent = nodes.getOrNull(node.pid)
            node.parent?.apply {
                children.add(node)
            }
        }
        return nodes.first()
    }

    private val transform = Transform<TestNode>(getAttr = { node, name ->
        if (name == "_id") return@Transform node.id
        if (name == "_pid") return@Transform node.pid
        val value = node.attr[name] ?: return@Transform null
        if (value is JsonNull) return@Transform null
        value.intOrNull ?: value.booleanOrNull ?: value.content
    }, getName = { node -> node.attr["name"]?.content }, getChildren = { node ->
        node.children.asSequence()
    }, getParent = { node -> node.parent })

    @Test
    fun check_query() {
        val text = "@TextView[text^='跳过'] + LinearLayout TextView[text*=`跳转`]"
        val selector = Selector.parse(text)
        println("selector: $selector")
        println(selector.trackIndex)
        println(selector.tracks.toList())

        val snapshotNode = getTreeNode("snapshot-1693227637861.json")
        val targets = transform.querySelectorAll(snapshotNode, selector).toList()
        println("target_size: " + targets.size)
        println(targets.firstOrNull())
    }

    @Test
    fun check_quote() {
//        https://github.com/gkd-kit/inspect/issues/7
        val selector = Selector.parse("a[a='\\\\'] ")
        println("check_quote:$selector")
    }

    @Test
    fun check_escape() {
        val source =
            "[a='\\\"'][a=\"'\"][a=`\\x20\\n\\uD83D\\uDE04`][a=`\\x20`][a=\"`\u0020\"][a=`\\t\\n\\r\\b\\x00\\x09\\x1d`]"
        println("source:$source")
        val selector = Selector.parse(source)
        println("check_quote:$selector")
    }

    @Test
    fun check_seq() {
        println(
            listOf(1, 2, 3, 4, 5, 6, 7, 8).asSequence().filterIndexes(listOf(0, 1, 7, 10)).toList()
        )
        println(listOf(0).asSequence().filterIndexes(listOf(0, 1, 7, 10)).toList())
    }

    @Test
    fun check_tuple() {
        val source = "[_id=15] >(1,2,9) X + Z >(7+9n) *"
        println("source:$source")
        val selector = Selector.parse(source)
        println("check_quote:$selector")

        // https://i.gkd.li/import/13247733
        // 1->3, 3->21
        // 1,3->24
        val snapshotNode = getTreeNode("snapshot-1698997584508.json")
        val (x1, x2) = (1..6).toList().shuffled().subList(0, 2).sorted()
        val x1N =
            transform.querySelectorAll(snapshotNode, Selector.parse("[_id=15] >$x1 *")).count()
        val x2N =
            transform.querySelectorAll(snapshotNode, Selector.parse("[_id=15] >$x2 *")).count()
        val x12N = transform.querySelectorAll(snapshotNode, Selector.parse("[_id=15] >($x1,$x2) *"))
            .count()

        println("$x1->$x1N, $x2->$x2N, ($x1,$x2)->$x12N")
    }

    @Test
    fun check_descendant() {
        // ad_container 符合 quickFind, 目标节点 tt_splash_skip_btn 在其内部但不符合 quickFind
        val source =
            "@[id=\"com.byted.pangle.m:id/tt_splash_skip_btn\"] <<n [id=\"com.coolapk.market:id/ad_container\"]"
        println("source:$source")
        val selector = Selector.parse(source)
        println("selector:$selector")
        // https://i.gkd.li/import/13247610
        val snapshotNode = getTreeNode("snapshot-1698990932472.json")
        println("result:" + transform.querySelectorAll(snapshotNode, selector).map { n -> n.id }
            .toList())
    }
}