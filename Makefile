# Makefile for most Java-based RoSuDa projects
# $Id$
#
# Note that some projects may be better compiled using xcodebuild

IGLOBAL_SRC:=$(wildcard rosuda/util/*.java)
# PoGraSS must be generated manually, because SVG is optional
POGRASS_SRC:=rosuda/pograss/PoGraSS.java rosuda/pograss/PoGraSSPS.java rosuda/pograss/PoGraSSPDF.java rosuda/pograss/PoGraSSmeta.java rosuda/pograss/PoGraSSgraphics.java
# variables with XTREME suffix use JOGL for OpenGL
POGRASS_SRC_XTREME:=$(POGRASS_SRC) rosuda/pograss/PoGraSSjogl.java
IBASE_SRC_RAW:= $(IGLOBAL_SRC) $(wildcard rosuda/ibase/*.java) $(wildcard rosuda/ibase/plots/*.java) $(wildcard rosuda/ibase/toolkit/*.java) $(POGRASS_SRC) rosuda/plugins/Plugin.java rosuda/plugins/PluginManager.java
IBASE_SRC_XTREME:=rosuda/ibase/toolkit/PGSJoglCanvas.java
IBASE_SRC:=$(filter-out $(IBASE_SRC_XTREME),$(IBASE_SRC_RAW))
KLIMT_SRC:=$(wildcard rosuda/klimt/*.java) $(wildcard rosuda/klimt/plots/*.java)
PLUGINS_SRC:=$(wildcard rosuda/plugins/*.java)
JRCLIENT_SRC:=$(wildcard rosuda/JRclient/*.java)
IPLOTS_SRC:=$(wildcard rosuda/iplots/*.java)
IWIDGETS_SRC:=$(wildcard rosuda/iWidgets/*.java)
JAVAGD_SRC:=$(wildcard rosuda/javaGD/*.java)
JGR_SRC:=$(wildcard rosuda/JGR/*.java) $(wildcard rosuda/JGR/toolkit/*.java) $(wildcard rosuda/JGR/util/*.java) $(wildcard rosuda/JGR/rhelp/*.java) $(wildcard rosuda/JGR/robjects/*.java) 
JRI_SRC:=$(wildcard rosuda/JRI/*.java)
CLASSPATH_XTREME:=rosuda/projects/klimt/jogl.jar

ifneq ($(shell uname),Darwin)
# remove all references to Mac platform as those classes can be compiled on a Mac only
IBASE_SRC:=$(filter-out %PlatformMac.java,$(IBASE_SRC))
KLIMT_SRC:=$(filter-out %PlatformMac.java,$(KLIMT_SRC))
IGLOBAL_SRC:=$(filter-out %PlatformMac.java,$(IGLOBAL_SRC))
IPLOTS_SRC:=$(filter-out %PlatformMac.java,$(IPLOTS_SRC))
endif

TARGETS=JRclient.jar ibase.jar klimt.jar iplots.jar iwidgets.jar JGR.jar Mondrian.jar

JAVAC=javac $(JFLAGS)

all: $(TARGETS)

define can-with-jar
	rm -rf org
	$(JAVAC) -d . $^
	jar fc $@ org
	rm -rf org	
endef

Mondrian.jar:
	make -C rosuda/Mondrian Mondrian.jar
	cp rosuda/Mondrian/Mondrian.jar .

JGR.jar: $(IBASE_SRC) $(JGR_SRC) $(IPLOTS_SRC) $(IWIDGETS_SRC) $(JRCLIENT_SRC) $(JRI_SRC) $(JAVAGD_SRC)
	rm -rf org
	$(JAVAC) -d . $^
	cp rosuda/projects/jgr/splash.jpg .
	cp -r rosuda/projects/jgr/icons .
	jar fcm $@ rosuda/projects/jgr/JGR.mft splash.jpg icons org
	rm -rf org splash.jpg icons

jgr-docs: $(JGR_SRC) 
	rm -rf JavaDoc
	mkdir JavaDoc
	javadoc -d JavaDoc -author -version -breakiterator -link http://java.sun.com/j2se/1.4.2/docs/api $^


ibase.jar: $(IBASE_SRC)
	$(can-with-jar)

JRclient.jar: $(JRCLIENT_SRC)
	$(can-with-jar)

klimt.jar: $(IBASE_SRC) $(KLIMT_SRC) $(PLUGINS_SRC) $(JRCLIENT_SRC)
	rm -rf org
	$(JAVAC) -d . $^
	cp rosuda/projects/klimt/splash.jpg .
	jar fcm $@ rosuda/projects/klimt/Klimt.mft splash.jpg org
	rm -rf org splash.jpg

klimt-docs: $(IBASE_SRC) $(KLIMT_SRC) $(PLUGINS_SRC) $(JRCLIENT_SRC)
	rm -rf JavaDoc
	mkdir JavaDoc
	javadoc -d JavaDoc -author -version -breakiterator -link http://java.sun.com/j2se/1.4.2/docs/api $^

iplots.jar: $(IBASE_SRC) $(IPLOTS_SRC)
	$(can-with-jar)

iwidgets.jar: iplots.jar $(IWIDGETS_SRC)
	rm -rf org
	$(JAVAC) -d . -classpath iplots.jar $(IWIDGETS_SRC)
	jar fc $@ org
	rm -rf org

clean:
	rm -rf $(TARGETS) org JavaDoc *~ rtest.class TextConsole.class
	make -C rosuda/Mondrian clean

.PHONY: clean all

