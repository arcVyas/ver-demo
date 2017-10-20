
def taskIn = args[0]
def branchIn = args[1]
def bump = args.length>2?args[2]:null

def taskMatrix={}
taskMatrix["snapshot"]=["develop"]
taskMatrix["candidate"]=["release"]
taskMatrix["final"]=["release","master", "hotfix"]

def cantBump=['hotfix']

class Globals{
  static boolean majorBump=false
  static boolean minorBump=false
  static boolean patchBump=false

  static def isBumped(){
    return majorBump || minorBump || patchBump
  }
} 

def branch = createBranchObj(branchIn)
branch.show()

if(bump!=null){
  if(cantBump.contains(branch.type)){
      println "can't bump up version on ${branch.type} branch"
    }else{
      if(bump=="major"){
        Globals.majorBump=true
      }else if(bump=="minor"){
        Globals.minorBump=true
      }else if(bump=="patch"){
        Globals.patchBump=true
    }
  }
}

if(!taskMatrix[taskIn].contains(branch.type)){
  println "Can't run $taskIn on ${branch.type} branch!"
  return
}

def version= getNewVersion(branch,taskIn)
version.show()

tagRepo(taskIn,version)

def tagRepo(task, version){
  def tagName
  if(task=="candidate"){
    tagName = "v${version.major}.${version.minor}.${version.patch}-rc.${version.rc}"
  }else if(task=="final"){
    tagName = "v${version.major}.${version.minor}.${version.patch}"
  }else{
    println "no tagging"
    return
  }
  println "Gonna tag - $tagName"
  ("git tag $tagName").execute()
  ("git push origin $tagName").execute()
  println "Tagged - $tagName"
}

def addSuffix(version,branch){
  if(branch=="feature" || branch=="bugfix" || branch=="other"){
    version.suffix
  }
}

def getNewVersion(branch, task){

  def gettags = ("git ls-remote -t -h").execute()
  def tags= readTags(gettags)

  def latestTag = tags[tags.size()-1]
  println "latest tag = $latestTag"

  def currentVer = createVersionObj(latestTag)

  switch (branch.type){
    case "master":
      currentVer.minor = currentVer.minor +1
      currentVer.patch = 0
      currentVer.rc=0
      break
    case "develop":
      currentVer.minor = currentVer.minor +1
      currentVer.patch = 0
      currentVer.rc=0
      currentVer.suffix = "-SNAPSHOT"
      break
    case "feature":
      currentVer.minor = currentVer.minor +1
      currentVer.patch = 0
      currentVer.rc=0
      currentVer.suffix = "-feature_"+branch.name
      break
    case "bugfix":
      currentVer.minor = currentVer.minor +1
      currentVer.patch = 0
      currentVer.rc=0
      currentVer.suffix = "-bugfix_"+branch.name
      break
    case "release":
      currentVer.minor = currentVer.minor +1
      currentVer.patch = 0
      if(task=="candidate"){
        currentVer.rc = currentVer.rc+1
        currentVer.suffix="-rc."+currentVer.rc
      }
      break
    case "hotfix":
      currentVer.patch = currentVer.patch +1
      currentVer.rc=0
      break
  }

  if(Globals.majorBump){
    currentVer.major = currentVer.major+1
    currentVer.minor=0
    currentVer.patch=0
    currentVer.rc=0
  }
  if(Globals.minorBump){
    currentVer.minor = currentVer.minor+1
    currentVer.patch=0
    currentVer.rc=0
  }
  if(Globals.patchBump){
    currentVer.patch = currentVer.patch+1
    currentVer.rc=0
  }
  currentVer
}

def createBranchObj(branch){
  def x = branch.split("/")
  def branchObj
  if(x.length>1){
    branchObj = new Branch(type:x[0], name:x[1])
  }else{
    branchObj = new Branch(type:x[0], name:x[0])
  }
  branchObj
}

def createVersionObj(latestTag){
  def m
  def versionObj
  if(latestTag.contains('rc')){
    m= latestTag =~ /v([0-9]*)\.([0-9]*)\.([0-9]*)\-rc\.([0-9]*)/;
    versionObj = new Version(major:m[0][1] as int, minor: m[0][2] as int, patch:m[0][3] as int, rc:m[0][4] as int)
  }else{
    m= latestTag =~ /v([0-9]*)\.([0-9]*)\.([0-9]*)/;
    versionObj = new Version(major:m[0][1] as int, minor: m[0][2] as int, patch:m[0][3] as int)
  }  
  versionObj
}


def readTags(gettags){

  return gettags.text.readLines()
         .collect { it.split()[1].replaceAll('refs/tags/', '')  }
         .unique()
         .findAll { it ==~ /v[0-9]*\.[0-9]*\.[0-9]*/ || it ==~ /v[0-9]*\.[0-9]*\.[0-9]*\-rc\.[0-9]*/ }
}

class Version{
  int major=0
  int minor=0
  int patch=0
  int rc=0
  String suffix=""
  def show(){
    println "Version: major:$major, minor:$minor, patch:$patch, rc:$rc, suffix:$suffix"
  }
}

class Branch{
  String type
  String name
  def show(){
    println "Branch: type:$type, name:$name"
  }
}