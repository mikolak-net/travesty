package net.mikolak.travesty.render

private[travesty] object PackageNameSimplifier {

  def apply(fullName: String): String = simplificationPrefixList.foldRight(fullName) {
    case (regex, name) => name.replaceAll(regex, "")
  }

  private val simplificationPrefixList = List(raw"scala\.(.+?)\.", raw"akka\.stream(\.(scala|java)dsl)?\.")
}
