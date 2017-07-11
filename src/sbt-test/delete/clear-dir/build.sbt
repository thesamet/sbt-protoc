PB.targets in Compile := Seq(PB.gens.java -> (sourceManaged in Compile).value)

commands += existsSource
val existsSource: Command = Command.single("existsSource"){
  (state: State, path: String) =>
    val extracted = Project.extract(state)
    val srcManaged = extracted.get(sourceManaged)
    val targetFile = srcManaged./(path)
    if (!targetFile.exists())
      sys.error(s"File $path does not exist in ${srcManaged.getAbsolutePath}")
    state
}
