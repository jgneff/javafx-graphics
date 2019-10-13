# Monocle EPD

This repository is a NetBeans project that contains the JavaFX Graphics module and uses Apache Ant to build its modular JAR file (*javafx.graphics.jar*). I use this project to edit, compile, and test my changes to the [Monocle EPD](https://github.com/javafxports/openjdk-jfx/issues/339) platform before copying the modified files into my fork of the official [JavaFX repository](https://github.com/openjdk/jfx).

## Project Settings

This project's Java Platform is the default platform, which I define as [OpenJDK 13](https://jdk.java.net/13/) for my installation of [NetBeans 11.2](https://snapcraft.io/netbeans). You can set up the project's dependency on the JavaFX SDK as described below.

### JavaFX SDK

Download the [Early-Access Build](https://gluonhq.com/products/javafx/) of the JavaFX Linux SDK and the JavaFX Documentation:

```
~/Downloads/openjfx-14-ea+1_linux-x64_bin-sdk.zip
~/Downloads/openjfx-14-ea+1-javadoc.zip
```

Extract the JavaFX Linux SDK into `~/lib` with:

```ShellSession
$ mkdir ~/lib
$ cd ~/lib
$ unzip ~/Downloads/openjfx-14-ea+1_linux-x64_bin-sdk.zip
```

Extract the JavaFX Sources into `~/lib/javafx-sdk-14/src` with:

```ShellSession
$ cd ~/lib/javafx-sdk-14
$ mkdir src
$ cd src
$ unzip ../lib/src.zip
```

Extract the JavaFX Documentation into `~/lib/javafx-sdk-14/doc` with:

```ShellSession
$ cd ~/lib/javafx-sdk-14
$ unzip ~/Downloads/openjfx-14-ea+1-javadoc.zip
$ mv openjfx-14-ea+1-javadoc doc
```

Those steps should resolve any problems with the project in NetBeans. If not, check the Modulepath as described below.

### Modulepath

Add the JavaFX modular JAR files to the project's Compile Modulepath. To do so, navigate to the Properties > Libraries > Compile tab, click the "+" sign to the right of **Modulepath**, select "Add JAR/Folder," and add the following six files:

```
~/lib/javafx-sdk-14/lib/javafx.base.jar
~/lib/javafx-sdk-14/lib/javafx.controls.jar
~/lib/javafx-sdk-14/lib/javafx.fxml.jar
~/lib/javafx-sdk-14/lib/javafx.media.jar
~/lib/javafx-sdk-14/lib/javafx.swing.jar
~/lib/javafx-sdk-14/lib/javafx.web.jar
```

**Note:** Do **not** add *javafx.graphics.jar* or *javafx-swt.jar*. This project builds the JavaFX Graphics module, and the SWT file is not a module JAR file.

Select each item in the Modulepath and click the Edit button to add the Sources directory to its corresponding modular JAR file:

```
~/lib/javafx-sdk-14/src/javafx.base
~/lib/javafx-sdk-14/src/javafx.controls
~/lib/javafx-sdk-14/src/javafx.fxml
~/lib/javafx-sdk-14/src/javafx.media
~/lib/javafx-sdk-14/src/javafx.swing
~/lib/javafx-sdk-14/src/javafx.web
```

Select each item in the Modulepath and click the Edit button to add the Javadoc directory to its corresponding modular JAR file:

```
~/lib/javafx-sdk-14/doc/javafx.base
~/lib/javafx-sdk-14/doc/javafx.controls
~/lib/javafx-sdk-14/doc/javafx.fxml
~/lib/javafx-sdk-14/doc/javafx.media
~/lib/javafx-sdk-14/doc/javafx.swing
~/lib/javafx-sdk-14/doc/javafx.web
```
