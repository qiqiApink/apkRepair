# apkRepair

## Introduction

安卓目前是全球第一大智能手机操作系统，拥有丰富的应用程序。同时这些应用程序也越来越复杂，安卓应用程序发开人员为了简化发开流程往往会使用大量的第三方代码库来丰富应用程序的功能。然而，在第三方库给开发带来便捷之外，却带来了大量的严重安全隐患，研究表明，大部分安卓应用程序的漏洞均来自于其所使用的第三方库。因此，即使治修复这些第三方库中的一小部分漏洞，也能给用户和商家带来很大的价值。所以我们设计了一款可以对安卓应用程序apk安装包进行Java第三方库漏洞检测并自动修复的工具。

对于Java第三方库的检测部分我们借鉴了[LibScout](https://github.com/reddr/LibScout)这一工具。LibScout是一种轻巧有效的静态分析工具，可检测Android / Java应用程序中的第三方库。该检测可抵抗常见的字节码混淆技术（例如标识符重命名）或基于代码的混淆（例如基于反射的API隐藏或控制流随机化）。此外，LibScout能够查明确切的库版本（在某些情况下为一组2-3个候选版本）。LibScout检测第三方库需要预先对原始库SDK（已编译的.jar / .aar文件）提取可用于在Android应用上进行检测的profile文件，profile文件使用VLIB格式。

我们为了达到检测第三方库漏洞的目的，提前收集了大量有关第三方库漏洞的信息，并对这些存在漏洞的不同版本的第三方库生成profile文件，用于LibScout对apk文件的检测。当前我们已经生成了在安卓应用程序中较为常见的漏洞对应一些第三方库不同版本的profile文件，存放于`./profile/`下。同时，我们还需要对这些不同版本的第三方库生成其对应的patch文件用于自动修复，我们采用的修复方法是根据第三方库所含漏洞发布的Github Commit得到程序补丁源码，将这些.java文件编译为一个.dex文件，该.dex文件即为该三方库的patch文件。对于检测到漏洞的apk文件，我们要根据其所含的存在漏洞的三方库的patch文件定位需要进行修改的文件具体在apk中的位置，并对应用该patch文件的兼容性进行检测，如果通过则应用该patch文件。应用patch文件的过程为，对patch文件和之前apk中定位到的需要被替换的文件反编译为.smali文件，并一一对应替换，即将patch文件中的.smali文件替换到apk中，将apk中原本的.smali文件删除。最后，我们对其重新签名并打包为apk文件，即为修复好的应用程序。

### Tool Design

* 预处理模块：收集漏洞信息，生成三方库profile文件及patch文件

* 漏洞检测模块：检测apk中包含的存在漏洞的三方库

* 漏洞定位模块：定位漏洞存在于apk中的位置，即需要修改的文件的位置

* 漏洞修复模块：应用patch文件，对apk进行漏洞修复，并重新签名打包

## Structure

```
.
├── android-sdk
│   └── android-28.jar
├── build.gradle (generate runnable apkRepair.jar)
├── config
│   ├── LibScout.toml (LibScout's config file)
│   └── logback.xml (log4j configuration file)
├── libs
│    dependencies
├── my-lib-repo
│    tpl .jar / .aar file and .xml file
├── patch
│    dex patch files(*.dex)
├── patchedAPK
│    output directory: patched apk files
├── profile
│    tpl profile(*.libv)
├── profile.sh (generate profiles for libraries)
├── Sdk
│    dx tool
└── src
     source directory of apkRepair
```

## Install

### Requirement

* Java 1.8 or higher

* Gradle 4.4.2 or higher

### Usage

```
$ gradle build
```

生成的apkRepair.jar可执行文件位于`./build/libs/`下

## Run

```
$ java -jar ./build/libs/apkRepair.jar path_to_apk
```

传入apk文件相对路径，运行后在`patchedApk/`下生成目标apk文件

**Example:** 假设在当前目录下有一个名称为com.tumblr.apk的应用程序安装包，运行`$ java -jar ./build/libs/apkRepair.jar ./com.tumblr.apk`后，将在`./patchedAPK/`下生成一个新的com.tumblr.apk文件，该文件名称与原apk文件相同，如果原apk存在漏洞，则该文件为修复后的apk文件，如果不存在漏洞，则该文件与原apk文件完全相同。

## Usage

本工具的两个最主要的模块漏洞检测模块和漏洞修复模块分别依赖于存在漏洞的三方库的profile文件及patch文件，我们可以根据需要对这两个部分进行扩充。

* profile文件：存放于`./profile/`下，用于漏洞检测

* patch文件：存放于`./patch/`下，用于漏洞修复

扩充一个漏洞三方库需要两步，第一步是生成该三方库的profile文件（[Add profile](#add-profile)），第二步是生成该三方库的补丁文件（[Add patch](#add-patch)）。

### Add profile

* 使用LibScout

* 库文件仅支持.jar或.aar格式，需自行从maven-central等仓库进行下载

* 将库文件(.jar / .aar)和库描述文件(.xml)放在`./my-lib-repo/`下

  **Example:** 在`./my-lib-repo/`下有commons-collections__commons-collections_3.2.1.jar和commons-collections__commons-collections_3.2.1.xml这两个文件，分别为commons-collections::commons-collections:3.2.1这个三方库的库文件和库描述文件

* 运行`$ java -jar ./libs/LibScout.jar -o profile -a ./android-sdk/android-28.jar -x "${xml_file}" "${lib_file}"`

  **Example:** 运行`$ java -jar ./libs/LibScout.jar -o profile -a ./android-sdk/android-28.jar -x my-lib-repo/commons-collections__commons-collections_3.2.1.xml my-lib-repo/commons-collections__commons-collections_3.2.1.jar`

* 批量添加多个三方库可运行`$ ./profile.sh`，该脚本会对`./my-lib-repo/`下所有的.jar文件及其对应的.xml文件生成.jar文件对应的三方库的profile文件

* 生成的profile文件（后缀均为.libv）存放于`./profiles/Vulnerable/`下，将`./profiles/Vulnerable/`下某一profile文件移入`./profile/`即可添加该profile文件对应的三方库特定版本为需检测的漏洞三方库

#### Library File Name Format

```
groupID__artefactID_version.jar or .aar
```

> Example: commons-collections__commons-collections_3.2.1.jar

#### Library Description XML File Name Format

文件名应和库文件名相同

> Example: commons-collections__commons-collections_3.2.1.xml

```xml
<?xml version="1.0"?>
<library>
    <name>commons-collections__commons-collections</name>

    <category>Vulnerable</category>

    <version>3.2.1</version>

    <releasedate>15.04.2008</releasedate>

    <comment></comment>
</library>
```

#### Library Profile File Name Format

保持默认即可，即与库文件名和库描述文件名相同

> Example: commons-collections__commons-collections_3.2.1.libv

### Add patch

patch文件的生成需要手动完成，并需要将生成patch文件放在`./patch/`下

在[Snyk网站漏洞库](https://snyk.io/vuln?type=maven)中查找相应的三方库漏洞的漏洞信息，其中包含Github Commit的链接，在Github Commit中我们可以获取到java文件的源码补丁，将源码补丁下载到本地，用`javac`命令将.java源文件编译成.class文件，再使用dx工具将.class文件编译成.dex文件，使用dx工具需要先下载Android Sdk，dx工具一般存放于`sdk/build-tools/version/`下。

**Note:**

* 编译.java源码补丁为.class时，可以将在预处理模块生成三方库profile文件过程中所使用的库文件.jar / .aar作为依赖来编译这些零散的.java源码

  Example: `$ javac -cp .:commons-collections__commons-collections_3.2.1.jar InvokerTransformer.java`
  
* 我们要将生成的.class文件替换到上一步所使用的.jar中，否则在使用dx工具进行编译时，会报找不到路径的错误

* 多个.class文件编译成一个.dex文件

#### dx Usage:

```
$ ./dx --dex --output=ouput_dex_file_name class_file_or_files
```

Example:

* 单个.class文件：`$ ./dx --dex --output=commons-collections__commons-collections_3.2.1.dex org/apache/commons/collections/functors/InvokerTransformer.class`

* 多个.class文件：`$ ./dx --dex --output=commons-collections__commons-collections_3.2.1.dex org/apache/commons/collections/functors/ForClosure.class org/apache/commons/collections/functors/InstantiateFactory.class`

#### Patch File Name Format

```
groupID__artefactID_version.dex
```

> Example: commons-collections__commons-collections_3.2.1.dex

这里的version是patch所能被使用的三方库的最高版本。同一个三方库的不同版本包含的漏洞可能不同，所以我们要对每一个版本生成其对应的patch文件。但是连续版本之间可能patch文件相同，为了节约存储空间，相同patch文件之间仅保留一个，同时在这种情况下版本号是连续的，所以将其命名格式中的版本号设为适用该patch文件的最大版本号。使用中根据apk检测出来的三方库及其对应版本，选择不同的patch文件进行修复。

Example: 以commons-collections::commons-collections这个库为例，版本\[,3.2.2)包含CVE-2015-6420这个漏洞，除此之外版本\[3.0,3.2.2)还存在CVE-2015-4852这个漏洞。很容易知道，版本\[,3.0)对应一个patch文件命名为commons-collections__commons-collections_2.1.1.dex（在maven-central仓库中我们可以查到版本3.0之前最大版本号为2.1.1），版本\[3.0,3.2.2)对应另一个patch文件，命名为commons-collections__commons-collections_3.2.1.dex。
