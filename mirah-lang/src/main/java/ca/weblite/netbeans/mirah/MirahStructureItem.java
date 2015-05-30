package ca.weblite.netbeans.mirah;

import ca.weblite.netbeans.mirah.lexer.MirahParser.NBMirahParserResult;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import mirah.lang.ast.Annotated;
import mirah.lang.ast.Annotation;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Constant;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Identifier;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.OptionalArgument;
import mirah.lang.ast.OptionalArgumentList;
import mirah.lang.ast.RequiredArgument;
import mirah.lang.ast.RequiredArgumentList;
import mirah.lang.ast.StaticMethodDefinition;
import mirah.lang.ast.TypeName;
import mirah.lang.ast.TypeNameList;
import mirah.lang.ast.TypeRefImpl;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.HtmlFormatter;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;

/**
 * Элемент, отображаемый в окне навигатора.
 * 
 * @author shannah
 */
public class MirahStructureItem implements StructureItem, ElementHandle {

    private static Modifier getModifier(mirah.lang.ast.Modifier mod) {
        switch (mod.value()) {
            case "PUBLIC":
                return Modifier.PUBLIC;
            case "PRIVATE":
                return Modifier.PRIVATE;
            case "PROTECTED":
                return Modifier.PROTECTED;
            case "STATIC":
                return Modifier.STATIC;
            case "DEPRECATED":
                return Modifier.DEPRECATED;
            case "ABSTRACT":
                return Modifier.ABSTRACT;
            default:
                return null;
        } 
    }
    
    private static boolean isDeprecated(Annotated annotated) {
        for (Iterator<Annotation> list = annotated.annotations().iterator(); list.hasNext();) {
            Annotation annotation = list.next();
            TypeName type = annotation.type();
            if (type instanceof Constant) {
                Constant constant = (Constant) type;
                if ("Deprecated".equals(constant.identifier()) || "DEPRECATED".equals(constant.identifier())) {
                    return true;
                }
            } else if (type instanceof TypeRefImpl) {
                TypeRefImpl typeRef = (TypeRefImpl) type;
                if ("Deprecated".equals(typeRef.name()) || "DEPRECATED".equals(typeRef.name())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    Snapshot snapshot;
    NBMirahParserResult.Block block;
    List<MirahStructureItem> children = null;
    
    public MirahStructureItem(Snapshot snapshot, NBMirahParserResult.Block item){
        this.snapshot = snapshot;
        this.block = item;
    }
    
    public Node getNode() {
        return block.getNode();
    }
    
    @Override
    public String getName() {
        if (block.getNode() instanceof MethodDefinition) {
            MethodDefinition method = (MethodDefinition) block.getNode();
            RequiredArgumentList args = method.arguments().required();
            StringBuilder argString = new StringBuilder();
            if (args.size() > 0) {
                argString.append(" (");
            }
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    argString.append(", ");
                }
                RequiredArgument argument = args.get(i);
                argString.append(argument.name().identifier());
                TypeName type = argument.type();
                if (type instanceof Constant) {
                    Constant constant = (Constant) type;
                    argString.append(':').append(constant.identifier());
                }                
            }
            if (args.size() > 0) {
                argString.append(')');
            }
            TypeName type = method.type();
            if (type instanceof Identifier) {
                Identifier identifier = (Identifier) type;
                return block.getDescription() + argString.toString() + " : " + identifier.identifier();
            } else {
                return block.getDescription() + argString.toString() + " : Object";                
            }
        } else if (block.getNode() instanceof ClassDefinition) {
            ClassDefinition clazz = (ClassDefinition) block.getNode();
            TypeName type = clazz.superclass();
            if (type instanceof Constant) {
                Constant constant = (Constant) type;
                return block.getDescription() + " :: " + constant.identifier();
            } 
            // todo: интерфейсы
        }
        return block.getDescription().toString();
    }

    @Override
    public String getSortText() {
        // todo: возможно, стоит сортировать отдельно родные и унаследованные методы, либо в зависимости от режима
        return getName();
    }

    @Override
    public String getHtml(HtmlFormatter f) {
        try {
            if (block.getNode() instanceof MethodDefinition) {
                MethodDefinition method = (MethodDefinition) block.getNode();
                boolean deprecated = isDeprecated(method);
                if (deprecated) {
                    f.deprecated(true);
                }
                f.appendText(block.getDescription().toString());
                if (deprecated) {
                    f.deprecated(false);
                }
                RequiredArgumentList args = method.arguments().required();
                OptionalArgumentList opts = method.arguments().optional();
                if (args.size() + opts.size() > 0) {
                    f.appendHtml(" (");
                    f.parameters(true);
                }
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        f.appendHtml(", ");
                    }
                    RequiredArgument argument = args.get(i);
                    f.appendText(argument.name().identifier());
                    TypeName type = argument.type();
                    if (type instanceof Identifier) {
                        Identifier identifier = (Identifier) type;
                        f.appendHtml(":");
                        f.appendText(identifier.identifier());
                    } else if (type instanceof TypeRefImpl) {
                        TypeRefImpl typeRef = (TypeRefImpl) type;
                        f.appendHtml(":");
                        f.appendText(typeRef.name());
                        if (typeRef.isArray()) {
                            f.appendHtml("[]");
                        }
                    }
                }
                for (int i = 0; i < opts.size(); i++) {
                    if (i > 0 || args.size() > 0) {
                        f.appendHtml(", ");
                    }
                    OptionalArgument argument = opts.get(i);
                    f.deprecated(true);
                    f.appendText(argument.name().identifier());
                    TypeName type = argument.type();
                    if (type instanceof Identifier) {
                        Identifier identifier = (Identifier) type;
                        f.appendHtml(":");
                        f.appendText(identifier.identifier());
                    } else if (type instanceof TypeRefImpl) {
                        TypeRefImpl typeRef = (TypeRefImpl) type;
                        f.appendHtml(":");
                        f.appendText(typeRef.name());
                        if (typeRef.isArray()) {
                            f.appendHtml("[]");
                        }
                    }
                    f.deprecated(false);
                }                
                if (args.size() + opts.size() > 0) {
                    f.parameters(false);
                    f.appendHtml(")");
                }
                TypeName type = method.type();
                f.appendHtml(" : ");
                f.parameters(true);
                if (type instanceof Identifier) {
                    Identifier identifier = (Identifier) type;
                    f.appendText(identifier.identifier());
                } else {
                    f.appendText("Object");
                }
                f.parameters(false);
                return f.getText();
            } else if (block.getNode() instanceof ClassDefinition) {
                ClassDefinition clazz = (ClassDefinition) block.getNode();
                boolean deprecated = isDeprecated(clazz);
                if (deprecated) {
                    f.deprecated(true);
                }
                f.emphasis(true);
                f.appendText(block.getDescription().toString());
                f.emphasis(false);                
                if (deprecated) {
                    f.deprecated(false);
                }
                TypeName superClass = clazz.superclass();
                if (superClass instanceof Constant) {
                    Constant constant = (Constant) superClass;
                    f.appendHtml(" &lt; ");
                    f.type(true);
                    f.appendText(constant.identifier());
                    f.type(false);
                }
                TypeNameList list = clazz.interfaces();
                for (int i = 0; i < list.size(); i++) {
                    if (i == 0) {
                        f.appendHtml(" :: ");                        
                    } else {
                        f.appendHtml(", ");
                    }
                    f.type(true);
                    TypeName ifs = list.get(i);
                    if (ifs instanceof Constant) {
                        Constant constant = (Constant) ifs;
                        f.appendText(constant.identifier());                        
                    } else if (ifs instanceof TypeRefImpl) {
                        TypeRefImpl typeRef = (TypeRefImpl) ifs;
                        f.appendText(typeRef.name());
                    }
                    f.type(false);
                }
                // todo: интерфейсы
                return f.getText();
            } else if (block.getNode() instanceof FieldDeclaration) {
                FieldDeclaration field = (FieldDeclaration) block.getNode();
                return getName();
            } else {
                return getName();
            }
        } catch (Exception e) {
            return getName();
        }
    }

    @Override
    public ElementHandle getElementHandle() {
        return this;
    }

    @Override
    public ElementKind getKind() {
        return block.getKind();
    }

    @Override
    public Set<Modifier> getModifiers() {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        if (block.getNode() instanceof MethodDefinition) {
            // todo: по идее, сюда же должен попадать случай ConstructorDefinition, но методы initialize пока не видны.
            // видны статические методы self.initialize
            MethodDefinition method = (MethodDefinition) block.getNode();
            for (Iterator<mirah.lang.ast.Modifier> it = method.modifiers().iterator(); it.hasNext();) {
                Modifier modifier = getModifier(it.next());
                if (modifier == null) {
                    continue;
                }
                result.add(modifier);
            }
            if (block.getNode() instanceof StaticMethodDefinition) {
                result.add(Modifier.STATIC);
            }
            if (isDeprecated(method)) {
                result.add(Modifier.DEPRECATED);                
            }
        } else if (block.getNode() instanceof ClassDefinition) {
            ClassDefinition clazz = (ClassDefinition) block.getNode();
            for (Iterator<mirah.lang.ast.Modifier> it = clazz.modifiers().iterator(); it.hasNext();) {
                Modifier modifier = getModifier(it.next());
                if (modifier == null) {
                    continue;
                }
                result.add(modifier);
            }
            if (isDeprecated(clazz)) {
                result.add(Modifier.DEPRECATED);
            }
        } else if (block.getNode() instanceof FieldDeclaration) {
            FieldDeclaration field = (FieldDeclaration) block.getNode();
            for (Iterator<mirah.lang.ast.Modifier> it = field.modifiers().iterator(); it.hasNext();) {
                Modifier modifier = getModifier(it.next());
                if (modifier == null) {
                    continue;
                }
                result.add(modifier);
            }            
            if (field.isStatic()) {
                result.add(Modifier.STATIC);
            }
            if (isDeprecated(field)) {
                result.add(Modifier.DEPRECATED);
            }
        }
        return result;
    }

    @Override
    public boolean isLeaf() {
        return getNestedItems().isEmpty();
    }

    @Override
    public List<? extends MirahStructureItem> getNestedItems() {
        // todo: унаследованные элементы
        if (children == null) {
            children = new ArrayList<>();
            for (NBMirahParserResult.Block child : block.getChildren()) {
                children.add(new MirahStructureItem(snapshot, child));
            }
        }
        return children;
    }

    @Override
    public long getPosition() {
        return block.getOffset();
    }

    @Override
    public long getEndPosition() {
        return block.getOffset() + block.getLength();
    }

    @Override
    public ImageIcon getCustomIcon() {
        return null;
    }

    @Override
    public FileObject getFileObject() {
        return snapshot.getSource().getFileObject();
    }

    @Override
    public String getMimeType() {
        return "text/x-mirah";
    }

    @Override
    public String getIn() {
        return getName();
    }
    
    @Override
    public boolean signatureEquals(ElementHandle eh) {
        if (!(eh instanceof MirahStructureItem)) {
            return false;
        }
        return eh.getName().equals(this.getName());
    }

    @Override
    public OffsetRange getOffsetRange(ParserResult result) {
        return new OffsetRange(block.getOffset(), block.getOffset() + block.getLength());
    }        
}
