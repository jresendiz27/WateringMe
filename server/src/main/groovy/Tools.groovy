class Tools {
  def static propertiesReader(def filePath){
    def props = new Properties()
    new File(filePath).withInputStream {
      stream -> props.load(stream) 
    }
    return props;
  }
  def static propertiesReaderFromStream(def stream){
    def props = new Properties()
    props.load(stream)
    return props;
  }

}
