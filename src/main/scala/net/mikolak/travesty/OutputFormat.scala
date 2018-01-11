package net.mikolak.travesty

import guru.nidi.graphviz.engine.{Format, Renderer}
import net.mikolak.travesty.render.TextDrawRenderer

sealed trait OutputFormat {
  private[travesty] def asJava: Format
}

sealed trait ImageFormat extends OutputFormat
object OutputFormat {
  case object PNG extends ImageFormat {
    private[travesty] val asJava = Format.PNG
  }
  case object SVG extends ImageFormat {
    private[travesty] val asJava = Format.SVG
  }
}

sealed trait TextFormat extends OutputFormat {
  private[travesty] def post: Renderer => String = _.toString
}
object TextFormat {

  case object JSON extends TextFormat {
    private[travesty] val asJava = Format.JSON
  }

  case object XDOT extends TextFormat {
    private[travesty] val asJava = Format.XDOT
  }

  case object Text extends TextFormat {
    private[travesty] val asJava = Format.XDOT

    override private[travesty] val post = TextDrawRenderer(_)
  }

}
