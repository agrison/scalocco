// **Scalocco** is a really quick-and-dirty, literate-programming-style documentation
// generator. It is a Scala port of [Docco](http://jashkenas.github.com/docco/),
// which was written by [Jeremy Ashkenas](https://github.com/jashkenas) in
// Coffescript and runs on node.js.
//
// Scalocco produces HTML that displays your comments alongside your code.
// Comments are passed through
// [Markdown](http://daringfireball.net/projects/markdown/syntax), and code is
// highlighted using [google-code-prettify](http://code.google.com/p/google-code-prettify/)
// syntax highlighting. This page is the result of running Scalocco against its
// own source file.
//
// Currently, to build Scalocco, you'll need **maven** and **scala**. The project
// depends on [scala-mustache](https://github.com/vspy/scala-mustache) and
// [Markdown4j](https://code.google.com/p/markdown4j/)
//
// To use Scalocco, run it from the command-line:
//
//     java -jar scalocco.jar .
//
// ...will generate linked HTML documentation for the named source files, saving
// it into a `docs` folder.
//
// The [source for Scalocco](http://github.com/agrison/scalacco) is available on GitHub (and also,
// on the right of this very webpage) and released under the MIT license.
/****************************************************************************************
 ______     ______     ______     __         ______     ______     ______     ______    
/\  ___\   /\  ___\   /\  __ \   /\ \       /\  __ \   /\  ___\   /\  ___\   /\  __ \   
\ \___  \  \ \ \____  \ \  __ \  \ \ \____  \ \ \/\ \  \ \ \____  \ \ \____  \ \ \/\ \  
 \/\_____\  \ \_____\  \ \_\ \_\  \ \_____\  \ \_____\  \ \_____\  \ \_____\  \ \_____\ 
  \/_____/   \/_____/   \/_/\/_/   \/_____/   \/_____/   \/_____/   \/_____/   \/_____/

                         Scala implementation of Docco
                         -----------------------------
         Produces HTML pages that displays your comments alongside your code.
****************************************************************************************/                                                                                        
package me.grison.scalocco

//### Imports ###
import java.io._
import io.Source
import java.util.UUID

//#### Import for processing Markdown ####
import com.petebevin.markdown.MarkdownProcessor;

//### Section class###
// The `Section` class is just an object having two fields to represent
//* the Scala code ;
//* the documentation that goes along with the previous code.
case class Section(doc: String, code: String)

//Markdown
//--------
class Markdown {
    class MarkdownableString(s: String) {
        // *Markdownify* the given text.
        def markdown = new MarkdownProcessor().markdown(s)
        // *Markdownify* the given text but removes the `<p/>` tags from the result
        def mkdNoP = markdown.replaceAll("</?p>", "").trim()
        // Creates a *Mustache* object whose template is the given String
        def mustache = new Mustache(s)
    }
    // Enrich the `String` class with two methods `markdown` and `mkdNoP`
    implicit def markdownStringWrapper(str: String) = new MarkdownableString(str)
}

//Scalocco
//---------------
object Scalocco extends Markdown {
    // This value is used to differentiate normal comments from scaladoc style.
    val scaladoc = UUID.randomUUID().toString

    //ScalaDoc Parsing
    //---------------
    object ScalaDocParser {
        /**
         * Abstract class representing a Scaladoc item.
         * @param tpl the Mustache template to be used to render the Scaladoc.
         */
        abstract case class DocItem(tpl: Mustache) {
            // Render this Scaladoc item
            def render = tpl.render(this)
        }

        /**
         * Scaladoc item representing the documentation itself
         * @param doc the global scaladoc documentation.
         */
        case class Doc(doc: String)
            extends DocItem("<tr><td colspan='2' class='doc'>{{{doc}}}</td></tr>".mustache)

        /**
         * Scaladoc item representing a param as described in the ScalaDoc
         * @param param the parameter name.
         * @param doc the documentation associated to this param.
         */
        case class Param(param: String, doc: String)
            extends DocItem("<tr><td class='param'><tt>{{param}}</tt></td><td class='param-doc'>{{{doc}}}</td></tr>".mustache)

        /**
         * Scaladoc item representing the return as described in the ScalaDoc
         * @param doc the documentation associated to the return.
         */
        case class Return(doc: String)
            extends DocItem("<tr><td class='return'>returns:</td><td>{{{doc}}}</td></tr>".mustache)

        /**
         * Sanitize the input by removing start tag, end tag and stars in the scaladoc.
         * @param s the String to sanitize.
         * @return the sanitized String with scaladoc special syntax removed.
         */
        def sanitize(s: String) : String =
            s.replaceAll("\\s*/?[*]+/?\\s+", " ").replaceAll("\\s+[*]\\s+", "").trim()

        /**
         * Parse the given String as a `List` of `DocItem`.
         * @param s the String to be parsed.
         * @return a `List` of `DocItem` representing the original argument.
         */
        def parse(s: String) : List[DocItem] = {
            s.split("[*]\\s*@(p|r)").toList.map(st =>
                sanitize(st).split("\\s+").toList match {
                    case "aram" :: t => Param(t.head, t.tail.mkString(" ").mkdNoP)
                    case "eturn" :: r => Return(r.mkString(" ").mkdNoP)
                    case x => Doc(x.mkString(" ").mkdNoP)
                }
            )
        }

        /**
         * Convert a String representing some Scaladoc to an HTML table showing its content.
         * @param s the Scaladoc String.
         * @return the HTML table representation of the given Scaladoc.
         */
        def toHtml(s: String) : String = {
            val sb: StringBuilder = new StringBuilder("<table class='doc'>")
            parse(s).foreach(item => sb.append(item.render))
            sb.append("</table>").toString()
        }
    }

    // Keep track of all the sources (as `File` objects) we are generating the documentation for.
    var sources = List[File]()

    /**
     * Returns a `Stream` of scala files found in the given directory.
     * @param dir the directory where to search for Scala and Java files.
     * @return a `Stream` of File objects.
     */
    def scalaFiles(dir: String): Stream[File] = {
        def files(f: File): Stream[File] = {
            f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(files) else Stream.empty)
        }
        files(new File(dir)).filter(_.getName.endsWith(".scala"))
    }

    /**
     * Parse the different sections of code, along with their respective comments.
     * @param source the source File
     * @return the list of sections
     */
    def parseSections(source: File): List[Section] = {
        /**
         * Transform the documentation into an HTML table if it's in Scaladoc style,
         * don't touch it otherwise
         * @param doc a StringBuilder containing the documentation
         */
        def scaladocIfNeeded(doc: StringBuilder) =
            if (doc.startsWith(scaladoc)) ScalaDocParser.toHtml(doc.substring(scaladoc.length)) else doc.toString

        // will hold the sections in the file
        var sections = List[Section]()
        // the `hasCode` variable remembers if we were reading code before a comment
        var (hasCode, inScalaDoc) = (false, false)
        // the two buffers, one for documentation, the other for scala code
        var (doc, code) = (new StringBuilder, new StringBuilder)
        // for each line in the source code
        Source.fromFile(source).getLines().foreach(line =>
            // Reading start of scaladoc `/**`
            if (line.matches("^\\s*/[*]{2}[^*]*")) {
                inScalaDoc = true
            // Reading end of scaladoc `*/`
            } else if (line.matches("[^*]*[*]/\\s*$")) {
                inScalaDoc = false;
            // Reading a comment line `//` or ` * ` if inside `Scaladoc`
            } else if (line.matches("^\\s*//.*") || (inScalaDoc && line.matches("^\\s*[*].*"))) {
                // if we did had code, store the code and documentation in the resulting section list
                if (hasCode) {
                    val documentation = scaladocIfNeeded(doc)
                    sections ::= Section(documentation, code.toString)
                    hasCode = false
                    doc = new StringBuilder
                    if (inScalaDoc) doc.append(scaladoc)
                    code = new StringBuilder
                }
                doc.append(line.replaceFirst("^\\s*//", "")).append("\n")
            } else {
                hasCode = true
                code.append(line).append("\n")
                inScalaDoc = false
            }
        )
        // don't forget what's in the buffer at the end
        sections ::= Section(scaladocIfNeeded(doc), code.toString)
        // return all the sections we did found
        sections.toList.reverse
    }

    /**
     * Generate the HTML documentation for the given File and sections.
     * @param source the file to be documented.
     * @param path the Path of the original file.
     * @param destPath the Path where to write the documentation file.
     */
    def documentFile(source: File, path: String, destPath: String) = {
        val sections = parseSections(source)
        val mustache = new Mustache(Source.fromURL(getClass.getResource("/template.html")).mkString)
        val html = mustache.render(Map(
            // This is the title of the Scala source file
            "title" -> source.getName,
            // The sources are available in the right-upper box on the generated documentation
            "sources" -> sources,
            // keep indexes for sections
            "sections" -> sections.zipWithIndex.map(t =>
                Map("index" -> t._2, "code" -> t._1.code, "doc" -> t._1.doc.markdown))
        ))
        val outputFile = new File(source.getCanonicalPath.replace(path, destPath) + ".html")
        println("Generating documentation: " + outputFile.getCanonicalPath)
        // output the HTML rendered with Mustache into a file named `SOURCE_FILE.scala.html`
        val p = new PrintWriter(outputFile)
        try { p.write(html) } finally { p.close() }
    }

    /**
     * Generate some documentation.
     * @param path the Path were we may find some scala files.
     * @param destPath the Path were we should output the documentation.
     */
    def generateDoc(path: String, destPath: String) = {
        val files = scalaFiles(path)
        sources = files.toList
        if (!new File(destPath).exists())
            new File(destPath).mkdirs()
        files.foreach(documentFile(_, path, destPath))
    }

    /**
     * Simply run **`scalocco`**.
     * @param args the arguments to the program
     */
    def main(args: Array[String]) {
        generateDoc(args(0), Option(args(1)).getOrElse("./docs/"))
    }
}
