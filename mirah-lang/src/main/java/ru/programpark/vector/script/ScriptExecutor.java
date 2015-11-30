package ru.programpark.vector.script;

import ca.weblite.asm.WLMirahCompiler;
import java.io.File;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import javax.tools.Diagnostic;
import mirah.lang.ast.StringCodeSource;
import org.mirah.jvm.compiler.BytecodeConsumer;
import org.mirah.jvm.mirrors.debug.ConsoleDebugger;
import org.mirah.jvm.mirrors.debug.DebugController;
import org.mirah.tool.MirahArguments;
import org.mirah.tool.MirahCompiler;
import org.mirah.util.SimpleDiagnostics;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;

class MyDiagnostics extends SimpleDiagnostics {
    public ArrayList<Diagnostic> diagns = new ArrayList <Diagnostic>();
    
    
    public MyDiagnostics() {
        super(false);
    }
//    @Override
//    public void log(Diagnostic.Diagnostic.Kind kind, String position, String message) {
//    }
    @Override
    public void report(Diagnostic dgnstc) {
        diagns.add(dgnstc);
    }
}

public class ScriptExecutor implements BytecodeConsumer {

    public ScriptExecutor() {
    }

    public void init_compiler(String args[]) {
        gen = 0;
        loader = new ByteClassLoader(this.getClass().getClassLoader());
        compiler_args = new MirahArguments();
        compiler_args.setup_logging();
        if (compiler_args.use_type_debugger() ? this.debugger == null : false) {
            ConsoleDebugger debugger = new ConsoleDebugger();
            debugger.start();
            this.debugger = debugger.debugger();
        }
        SimpleDiagnostics diagnostics = compiler_args.diagnostics();
        diagnostics.setMaxErrors(compiler_args.max_errors());
        java.net.URL classpath[] = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        compiler = new MirahCompiler(diagnostics, compiler_args.jvm_version(), classpath, compiler_args.real_bootclasspath(), compiler_args.real_macroclasspath(), compiler_args.destination(), compiler_args.real_macro_destination(), this.debugger, compiler_args.use_new_closures());
    }
    
    public void compileScript( FileObject root, FileObject fo ) 
    {
        try {
            WLMirahCompiler compiler = new WLMirahCompiler();
            compiler.setPrecompileJavaStubs(false);

            ClassPath compileClassPath = ClassPath.getClassPath(root, ClassPath.COMPILE);
            ClassPath buildClassPath = ClassPath.getClassPath(root, ClassPath.EXECUTE);
            ClassPath srcClassPath = ClassPath.getClassPath(root, ClassPath.SOURCE);
            ClassPath bootClassPath = ClassPath.getClassPath(root, ClassPath.BOOT);

            StringBuffer sb = new StringBuffer();
            if ( compileClassPath != null ) sb.append(compileClassPath.toString()).append(";");
            if ( buildClassPath != null ) sb.append(buildClassPath.toString()).append(";");
            if ( bootClassPath != null ) sb.append(bootClassPath.toString()).append(";");
            String cp = sb.toString();

            cp = "C:\\java-dao\\mirah-jfxui\\build\\classes\\main;C:\\java-dao\\mirah-jfxui\\build\\classes-mirah\\main;C:\\java-dao\\java-dao\\build\\classes\\main;C:\\java-dao\\mirah-dao-dsl\\build\\classes\\main;C:\\java-dao\\mirah-dao-dsl\\build\\classes-mirah\\main;C:\\java-dao\\java-rvec-server\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\com\\googlecode\\cqengine\\cqengine\\2.0.3\\cqengine-2.0.3.jar;C:\\java-dao\\mirah-utils\\build\\classes\\main;C:\\java-dao\\mirah-utils\\build\\classes-mirah\\main;C:\\java-dao\\common-deps\\build\\classes\\main;C:\\Users\\savushkin\\.gradle\\caches\\modules-2\\files-2.1\\com.oracle\\javafx\\2.1\\b417faa24bffca8cce849de80b7843298985731c\\javafx-2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\sdoapi\\1.0\\sdoapi-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\net\\openhft\\koloboke-api-jdk8\\0.6.7\\koloboke-api-jdk8-0.6.7.jar;C:\\Users\\savushkin\\.m2\\repository\\net\\openhft\\koloboke-impl-jdk8\\0.6.7\\koloboke-impl-jdk8-0.6.7.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-modules\\RELEASE802\\org-openide-modules-RELEASE802.jar;C:\\java-dao\\configuration\\build\\classes\\main;C:\\java-dao\\plugin-common\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-libs-xerces\\RELEASE802\\org-netbeans-libs-xerces-RELEASE802.jar;C:\\java-dao\\cayenne\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\org\\projectlombok\\lombok\\1.14.8\\lombok-1.14.8.jar;C:\\java-dao\\jgroups-transport\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\com\\googlecode\\concurrent-trees\\concurrent-trees\\2.4.0\\concurrent-trees-2.4.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\xerial\\sqlite-jdbc\\3.8.10.1\\sqlite-jdbc-3.8.10.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\esotericsoftware\\kryo\\3.0.0\\kryo-3.0.0.jar;C:\\Users\\savushkin\\.m2\\repository\\de\\javakaffee\\kryo-serializers\\0.28\\kryo-serializers-0.28.jar;C:\\Users\\savushkin\\.gradle\\caches\\modules-2\\files-2.1\\org.mirah\\mirahc\\0.1.5-10\\35a16f956f276322e3c7abe33e33f0bad25099a1\\mirahc-0.1.5-10.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\external\\org-apache-commons-logging\\RELEASE802\\org-apache-commons-logging-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\external\\org-apache-commons-codec\\RELEASE802\\org-apache-commons-codec-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\log4j\\log4j\\1.2.17\\log4j-1.2.17.jar;C:\\Users\\savushkin\\.m2\\repository\\log4j\\apache-log4j-extras\\1.2.17\\apache-log4j-extras-1.2.17.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-collections\\commons-collections\\3.2.1\\commons-collections-3.2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\poi\\poi-ooxml\\3.9\\poi-ooxml-3.9.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-io\\commons-io\\2.4\\commons-io-2.4.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\ojdbc7\\12.1.0.1\\ojdbc7-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\xdb6\\12.1.0.1\\xdb6-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\xmlparserv2\\12.1.0.1\\xmlparserv2-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\orai18n\\12.1.0.1\\orai18n-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\orai18n-mapping\\12.1.0.1\\orai18n-mapping-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\hsqldb\\hsqldb\\2.3.2\\hsqldb-2.3.2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\commons\\commons-dbcp2\\2.1\\commons-dbcp2-2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-pool\\commons-pool\\1.6\\commons-pool-1.6.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\zookeeper\\zookeeper\\3.4.6\\zookeeper-3.4.6.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\zookeeper\\zookeeper-recipes\\3.4.5.1\\zookeeper-recipes-3.4.5.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.6.4\\slf4j-api-1.6.4.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\slf4j\\slf4j-log4j12\\1.6.4\\slf4j-log4j12-1.6.4.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-vfs\\commons-vfs\\1.0\\commons-vfs-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\google\\guava\\guava\\18.0\\guava-18.0.jar;C:\\Users\\savushkin\\.m2\\repository\\de\\ruedigermoeller\\fst\\2.34\\fst-2.34.jar;C:\\Users\\savushkin\\.m2\\repository\\svg\\xml-apis\\1.0\\xml-apis-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\2.0.3\\jsr305-2.0.3.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-util\\RELEASE802\\org-openide-util-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-util-lookup\\RELEASE802\\org-openide-util-lookup-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-configuration\\commons-configuration\\1.10\\commons-configuration-1.10.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-cli\\commons-cli\\1.3.1\\commons-cli-1.3.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\modules\\org-netbeans-core\\RELEASE802\\org-netbeans-core-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-api-annotations-common\\RELEASE802\\org-netbeans-api-annotations-common-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-dialogs\\RELEASE802\\org-openide-dialogs-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-awt\\RELEASE802\\org-openide-awt-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-keyring\\RELEASE802\\org-netbeans-modules-keyring-RELEASE802.jar;C:\\java-dao\\bootstrap\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\external\\xerces-2.8.0\\RELEASE802\\xerces-2.8.0-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-tools\\4.0.M2\\cayenne-tools-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-project\\4.0.M2\\cayenne-project-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-server\\4.0.M2\\cayenne-server-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-client\\4.0.M2\\cayenne-client-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\jgroups\\jgroups\\3.6.2.Final\\jgroups-3.6.2.Final.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\esotericsoftware\\reflectasm\\1.10.0\\reflectasm-1.10.0.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\esotericsoftware\\minlog\\1.3.0\\minlog-1.3.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\objenesis\\objenesis\\2.1\\objenesis-2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\poi\\poi\\3.9\\poi-3.9.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\poi\\poi-ooxml-schemas\\3.9\\poi-ooxml-schemas-3.9.jar;C:\\Users\\savushkin\\.m2\\repository\\dom4j\\dom4j\\1.6.1\\dom4j-1.6.1.jar;C:\\Users\\savushkin\\.m2\\repository\\javax\\xml\\jsr173\\1.0\\jsr173-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\commons\\commons-pool2\\2.3\\commons-pool2-2.3.jar;C:\\Users\\savushkin\\.m2\\repository\\io\\netty\\netty\\3.7.0.Final\\netty-3.7.0.Final.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.5.3\\jackson-core-2.5.3.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\modules\\org-netbeans-bootstrap\\RELEASE802\\org-netbeans-bootstrap-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\modules\\org-netbeans-core-startup\\RELEASE802\\org-netbeans-core-startup-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-sampler\\RELEASE802\\org-netbeans-modules-sampler-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-swing-plaf\\RELEASE802\\org-netbeans-swing-plaf-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-actions\\RELEASE802\\org-openide-actions-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-explorer\\RELEASE802\\org-openide-explorer-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-filesystems\\RELEASE802\\org-openide-filesystems-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-io\\RELEASE802\\org-openide-io-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-loaders\\RELEASE802\\org-openide-loaders-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-nodes\\RELEASE802\\org-openide-nodes-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-windows\\RELEASE802\\org-openide-windows-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-api-progress\\RELEASE802\\org-netbeans-api-progress-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\ru\\programpark\\rvec\\jruby\\1.7.4-21\\jruby-1.7.4-21.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-logging\\commons-logging\\1.1\\commons-logging-1.1.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-dbcp\\commons-dbcp\\1.2.1\\commons-dbcp-1.2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\velocity\\velocity\\1.6.3\\velocity-1.6.3.jar;C:\\Users\\savushkin\\.m2\\repository\\foundrylogic\\vpp\\vpp\\2.2.1\\vpp-2.2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\net\\java\\dev\\inflector\\inflector\\0.7.0\\inflector-0.7.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-di\\4.0.M2\\cayenne-di-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\caucho\\resin-hessian\\3.1.6\\resin-hessian-3.1.6.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\ow2\\asm\\asm\\4.2\\asm-4.2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\xmlbeans\\xmlbeans\\2.3.0\\xmlbeans-2.3.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-text\\RELEASE802\\org-openide-text-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-swing-outline\\RELEASE802\\org-netbeans-swing-outline-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-swing-tabcontrol\\RELEASE802\\org-netbeans-swing-tabcontrol-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-editor-mimelookup\\RELEASE802\\org-netbeans-modules-editor-mimelookup-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-queries\\RELEASE802\\org-netbeans-modules-queries-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\oro\\oro\\2.0.8\\oro-2.0.8.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\javassist\\javassist\\3.19.0-GA\\javassist-3.19.0-GA.jar;C:\\java-dao\\samples\\build\\classes-mirah\\main;C:\\java-dao\\samples\\build\\classes\\main;C:\\java-dao\\samples\\build\\resources\\main"
            + "C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\resources.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\rt.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\sunrsasign.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\jsse.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\jce.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\charsets.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\jfr.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\classes;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\access-bridge-64.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\cldrdata.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\dnsns.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\jaccess.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\jfxrt.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\localedata.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\nashorn.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\sunec.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\sunjce_provider.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\sunmscapi.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\sunpkcs11.jar;C:\\Program Files\\Java\\jdk1.8.0_45\\jre\\lib\\ext\\zipfs.jar"
            + "C:\\java-dao\\mirah-jfxui\\build\\classes\\main;C:\\java-dao\\mirah-jfxui\\build\\classes-mirah\\main;C:\\java-dao\\java-dao\\build\\classes\\main;C:\\java-dao\\mirah-dao-dsl\\build\\classes\\main;C:\\java-dao\\mirah-dao-dsl\\build\\classes-mirah\\main;C:\\java-dao\\java-rvec-server\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\com\\googlecode\\cqengine\\cqengine\\2.0.3\\cqengine-2.0.3.jar;C:\\java-dao\\mirah-utils\\build\\classes\\main;C:\\java-dao\\mirah-utils\\build\\classes-mirah\\main;C:\\java-dao\\common-deps\\build\\classes\\main;C:\\Users\\savushkin\\.gradle\\caches\\modules-2\\files-2.1\\com.oracle\\javafx\\2.1\\b417faa24bffca8cce849de80b7843298985731c\\javafx-2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\sdoapi\\1.0\\sdoapi-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\net\\openhft\\koloboke-api-jdk8\\0.6.7\\koloboke-api-jdk8-0.6.7.jar;C:\\Users\\savushkin\\.m2\\repository\\net\\openhft\\koloboke-impl-jdk8\\0.6.7\\koloboke-impl-jdk8-0.6.7.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-modules\\RELEASE802\\org-openide-modules-RELEASE802.jar;C:\\java-dao\\configuration\\build\\classes\\main;C:\\java-dao\\plugin-common\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-libs-xerces\\RELEASE802\\org-netbeans-libs-xerces-RELEASE802.jar;C:\\java-dao\\cayenne\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\org\\projectlombok\\lombok\\1.14.8\\lombok-1.14.8.jar;C:\\java-dao\\jgroups-transport\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\com\\googlecode\\concurrent-trees\\concurrent-trees\\2.4.0\\concurrent-trees-2.4.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\xerial\\sqlite-jdbc\\3.8.10.1\\sqlite-jdbc-3.8.10.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\esotericsoftware\\kryo\\3.0.0\\kryo-3.0.0.jar;C:\\Users\\savushkin\\.m2\\repository\\de\\javakaffee\\kryo-serializers\\0.28\\kryo-serializers-0.28.jar;C:\\Users\\savushkin\\.gradle\\caches\\modules-2\\files-2.1\\org.mirah\\mirahc\\0.1.5-10\\35a16f956f276322e3c7abe33e33f0bad25099a1\\mirahc-0.1.5-10.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\external\\org-apache-commons-logging\\RELEASE802\\org-apache-commons-logging-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\external\\org-apache-commons-codec\\RELEASE802\\org-apache-commons-codec-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\log4j\\log4j\\1.2.17\\log4j-1.2.17.jar;C:\\Users\\savushkin\\.m2\\repository\\log4j\\apache-log4j-extras\\1.2.17\\apache-log4j-extras-1.2.17.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-collections\\commons-collections\\3.2.1\\commons-collections-3.2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\poi\\poi-ooxml\\3.9\\poi-ooxml-3.9.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-io\\commons-io\\2.4\\commons-io-2.4.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\ojdbc7\\12.1.0.1\\ojdbc7-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\xdb6\\12.1.0.1\\xdb6-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\xmlparserv2\\12.1.0.1\\xmlparserv2-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\orai18n\\12.1.0.1\\orai18n-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\oracle\\orai18n-mapping\\12.1.0.1\\orai18n-mapping-12.1.0.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\hsqldb\\hsqldb\\2.3.2\\hsqldb-2.3.2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\commons\\commons-dbcp2\\2.1\\commons-dbcp2-2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-pool\\commons-pool\\1.6\\commons-pool-1.6.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\zookeeper\\zookeeper\\3.4.6\\zookeeper-3.4.6.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\zookeeper\\zookeeper-recipes\\3.4.5.1\\zookeeper-recipes-3.4.5.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.6.4\\slf4j-api-1.6.4.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\slf4j\\slf4j-log4j12\\1.6.4\\slf4j-log4j12-1.6.4.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-vfs\\commons-vfs\\1.0\\commons-vfs-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\google\\guava\\guava\\18.0\\guava-18.0.jar;C:\\Users\\savushkin\\.m2\\repository\\de\\ruedigermoeller\\fst\\2.34\\fst-2.34.jar;C:\\Users\\savushkin\\.m2\\repository\\svg\\xml-apis\\1.0\\xml-apis-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\2.0.3\\jsr305-2.0.3.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-util\\RELEASE802\\org-openide-util-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-util-lookup\\RELEASE802\\org-openide-util-lookup-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-configuration\\commons-configuration\\1.10\\commons-configuration-1.10.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-cli\\commons-cli\\1.3.1\\commons-cli-1.3.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\modules\\org-netbeans-core\\RELEASE802\\org-netbeans-core-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-api-annotations-common\\RELEASE802\\org-netbeans-api-annotations-common-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-dialogs\\RELEASE802\\org-openide-dialogs-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-awt\\RELEASE802\\org-openide-awt-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-keyring\\RELEASE802\\org-netbeans-modules-keyring-RELEASE802.jar;C:\\java-dao\\bootstrap\\build\\classes\\main;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\external\\xerces-2.8.0\\RELEASE802\\xerces-2.8.0-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-tools\\4.0.M2\\cayenne-tools-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-project\\4.0.M2\\cayenne-project-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-server\\4.0.M2\\cayenne-server-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-client\\4.0.M2\\cayenne-client-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\jgroups\\jgroups\\3.6.2.Final\\jgroups-3.6.2.Final.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\esotericsoftware\\reflectasm\\1.10.0\\reflectasm-1.10.0.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\esotericsoftware\\minlog\\1.3.0\\minlog-1.3.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\objenesis\\objenesis\\2.1\\objenesis-2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\poi\\poi\\3.9\\poi-3.9.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\poi\\poi-ooxml-schemas\\3.9\\poi-ooxml-schemas-3.9.jar;C:\\Users\\savushkin\\.m2\\repository\\dom4j\\dom4j\\1.6.1\\dom4j-1.6.1.jar;C:\\Users\\savushkin\\.m2\\repository\\javax\\xml\\jsr173\\1.0\\jsr173-1.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\commons\\commons-pool2\\2.3\\commons-pool2-2.3.jar;C:\\Users\\savushkin\\.m2\\repository\\io\\netty\\netty\\3.7.0.Final\\netty-3.7.0.Final.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.5.3\\jackson-core-2.5.3.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\modules\\org-netbeans-bootstrap\\RELEASE802\\org-netbeans-bootstrap-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\modules\\org-netbeans-core-startup\\RELEASE802\\org-netbeans-core-startup-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-sampler\\RELEASE802\\org-netbeans-modules-sampler-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-swing-plaf\\RELEASE802\\org-netbeans-swing-plaf-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-actions\\RELEASE802\\org-openide-actions-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-explorer\\RELEASE802\\org-openide-explorer-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-filesystems\\RELEASE802\\org-openide-filesystems-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-io\\RELEASE802\\org-openide-io-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-loaders\\RELEASE802\\org-openide-loaders-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-nodes\\RELEASE802\\org-openide-nodes-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-windows\\RELEASE802\\org-openide-windows-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-api-progress\\RELEASE802\\org-netbeans-api-progress-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\ru\\programpark\\rvec\\jruby\\1.7.4-21\\jruby-1.7.4-21.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-logging\\commons-logging\\1.1\\commons-logging-1.1.jar;C:\\Users\\savushkin\\.m2\\repository\\commons-dbcp\\commons-dbcp\\1.2.1\\commons-dbcp-1.2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\velocity\\velocity\\1.6.3\\velocity-1.6.3.jar;C:\\Users\\savushkin\\.m2\\repository\\foundrylogic\\vpp\\vpp\\2.2.1\\vpp-2.2.1.jar;C:\\Users\\savushkin\\.m2\\repository\\net\\java\\dev\\inflector\\inflector\\0.7.0\\inflector-0.7.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\cayenne\\cayenne-di\\4.0.M2\\cayenne-di-4.0.M2.jar;C:\\Users\\savushkin\\.m2\\repository\\com\\caucho\\resin-hessian\\3.1.6\\resin-hessian-3.1.6.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\ow2\\asm\\asm\\4.2\\asm-4.2.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\apache\\xmlbeans\\xmlbeans\\2.3.0\\xmlbeans-2.3.0.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-openide-text\\RELEASE802\\org-openide-text-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-swing-outline\\RELEASE802\\org-netbeans-swing-outline-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-swing-tabcontrol\\RELEASE802\\org-netbeans-swing-tabcontrol-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-editor-mimelookup\\RELEASE802\\org-netbeans-modules-editor-mimelookup-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\netbeans\\api\\org-netbeans-modules-queries\\RELEASE802\\org-netbeans-modules-queries-RELEASE802.jar;C:\\Users\\savushkin\\.m2\\repository\\oro\\oro\\2.0.8\\oro-2.0.8.jar;C:\\Users\\savushkin\\.m2\\repository\\org\\javassist\\javassist\\3.19.0-GA\\javassist-3.19.0-GA.jar;C:\\java-dao\\samples\\build\\classes-mirah\\main;C:\\java-dao\\samples\\build\\classes\\main;C:\\java-dao\\samples\\build\\resources\\main"
            + "C:\\java-dao\\samples\\src\\main\\java;C:\\java-dao\\samples\\src\\main\\mirah;C:\\java-dao\\samples\\src\\main\\resources";
            

            compiler.setSourcePath(fo.getPath());
            compiler.setDestinationDirectory(new File("c:\\java-dao\\samples\\src\\main\\scripts"));
            MyDiagnostics myd = new MyDiagnostics();    
            SimpleDiagnostics sd = new SimpleDiagnostics(false);
//            compiler.setDiagnostics(new SimpleDiagnostics(false));
            compiler.setDiagnostics(myd);
            compiler.setDiagnostics(sd);
            compiler.setClassPath(cp);
            compiler.setMacroClassPath("");
            compiler.setBootClassPath(bootClassPath.toString());
            compiler.compile(new String[]{"--new-closures"});
            for( Diagnostic d : myd.diagns ) {
                String mm = d.getMessage(Locale.getDefault());
                int y = 0;
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    /*
    public static void main(String args[]) {
        Mirb c = new Mirb();
        c.init_compiler(args);
        Scanner s = new Scanner(System.in);
        System.out.println("Hint: Use [:e - eval text block|:c - compile text block|:n - new classloader|:q - quit] commands on a separate line");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = s.nextLine();
            if (line.equals(":e")) {
                c.eval(sb.toString());
                sb.setLength(0);
            } else if (line.equals(":c")) {
                c.compile(sb.toString());
                sb.setLength(0);
            } else if (line.equals(":n")) {
                c.init_compiler(args);
                sb.setLength(0);
            } else {
                if (line.equals(":q")) {
                    System.exit(0);
                }
                sb.append(line);
                sb.append("\n");
            }
        }
    }
    */
    public void eval(String code) 
    {
        try {
            gen = gen + 1;
            String scriptName = (new StringBuilder()).append(SCRIPT_PREFIX).append(gen).toString();
            code = (new StringBuilder()).append("class ").append(scriptName).append(" implements dsl.Script\n             def eval\n               ").append(code).append("\n             end\n        end").toString();
            if (call_compile(code) == 0) {
                ((Script) loader.findClass(scriptName).newInstance()).eval();
            }
        } catch (Throwable ex) {
            System.out.println("unable to eval: "+ex);
            ex.printStackTrace();
        }
    }

    public void compile(String code) {
        call_compile(code);
    }

    public int call_compile(String code) {
        try {
            compiler.parse(new StringCodeSource("DashE", code));
            compiler.infer();
            compiler.compile(this);
            return 0;
        } catch (Throwable ex) {
            System.out.println("Unable to compile: "+ex);
            ex.printStackTrace();
            return 1;
        }
    }

    public void consumeClass(String filename, byte bytes[]) {
        try {
            if (!loader.contains(filename)) {
                System.out.println("defining class: "+filename);
                loader.add(filename, bytes);
            }
        } catch (Throwable e) {
            System.out.println("Unable to eval: "+e);
            e.printStackTrace();
        }
    }

    private int gen;
    private static String SCRIPT_PREFIX = "Gen_S";
    private ByteClassLoader loader;
    private DebugController debugger;
    private MirahCompiler compiler;
    private MirahArguments compiler_args;
}
