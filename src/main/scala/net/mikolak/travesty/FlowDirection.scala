package net.mikolak.travesty

import guru.nidi.graphviz.attribute.RankDir

sealed trait FlowDirection {
  private[travesty] def asJava: RankDir
}
case object LeftToRight extends FlowDirection {
  private[travesty] val asJava = RankDir.LEFT_TO_RIGHT
}
case object RightToLeft extends FlowDirection {
  private[travesty] val asJava = RankDir.RIGHT_TO_LEFT
}
case object TopToBottom extends FlowDirection {
  private[travesty] val asJava = RankDir.TOP_TO_BOTTOM
}
case object BottomToTop extends FlowDirection {
  private[travesty] val asJava = RankDir.BOTTOM_TO_TOP
}
