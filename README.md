     ______     ______     ______     __         ______     ______     ______     ______    
    /\  ___\   /\  ___\   /\  __ \   /\ \       /\  __ \   /\  ___\   /\  ___\   /\  __ \   
    \ \___  \  \ \ \____  \ \  __ \  \ \ \____  \ \ \/\ \  \ \ \____  \ \ \____  \ \ \/\ \  
     \/\_____\  \ \_____\  \ \_\ \_\  \ \_____\  \ \_____\  \ \_____\  \ \_____\  \ \_____\ 
      \/_____/   \/_____/   \/_/\/_/   \/_____/   \/_____/   \/_____/   \/_____/   \/_____/
    
                             Scala implementation of Docco
                             -----------------------------
             Produces HTML pages that displays your comments alongside your code.

**Scalocco** is a really quick-and-dirty, literate-programming-style documentation
generator. It is a Scala port of [Docco](http://jashkenas.github.com/docco/),
which was written by [Jeremy Ashkenas](https://github.com/jashkenas) in
Coffescript and runs on node.js.

Scalocco produces HTML that displays your comments alongside your code.
Comments are passed through
[Markdown](http://daringfireball.net/projects/markdown/syntax), and code is
highlighted using [google-code-prettify](http://code.google.com/p/google-code-prettify/)
syntax highlighting.

Currently, to build Scalocco, you'll need **maven** and **scala**. The project
depends on [scala-mustache](https://github.com/vspy/scala-mustache) and
[Markdown4j](https://code.google.com/p/markdown4j/)

To use Scalocco, build it using `maven` (no `sbt` for now) then run it from the command-line:

     java -jar scalocco.jar /path/to/scala/files

...will generate linked HTML documentation for the named source files, saving
it into a `docs` folder.

The visual style was borrowed from the .Net implementation of Docco: [Nocco](https://github.com/dontangg/nocco)

You can see the result of running Scalocco on its own source code here: [http://grison.me/scalocco/scalocco.scala.html](http://grison.me/scalocco/scalocco.scala.html)