package ca.weblite.netbeans.mirah;

import mirah.lang.ast.Annotation;
import mirah.lang.ast.AnnotationList;
import mirah.lang.ast.Arguments;
import mirah.lang.ast.Array;
import mirah.lang.ast.AttrAssign;
import mirah.lang.ast.BindingReference;
import mirah.lang.ast.Block;
import mirah.lang.ast.BlockArgument;
import mirah.lang.ast.BlockPass;
import mirah.lang.ast.Boolean;
import mirah.lang.ast.Break;
import mirah.lang.ast.Builtin;
import mirah.lang.ast.Call;
import mirah.lang.ast.Case;
import mirah.lang.ast.Cast;
import mirah.lang.ast.CharLiteral;
import mirah.lang.ast.ClassAppendSelf;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.Colon2;
import mirah.lang.ast.Colon3;
import mirah.lang.ast.Constant;
import mirah.lang.ast.ConstantAssign;
import mirah.lang.ast.ConstructorDefinition;
import mirah.lang.ast.ElemAssign;
import mirah.lang.ast.EmptyArray;
import mirah.lang.ast.Ensure;
import mirah.lang.ast.ErrorNode;
import mirah.lang.ast.FieldAccess;
import mirah.lang.ast.FieldAssign;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Fixnum;
import mirah.lang.ast.Float;
import mirah.lang.ast.FunctionalCall;
import mirah.lang.ast.Hash;
import mirah.lang.ast.HashEntry;
import mirah.lang.ast.HashEntryList;
import mirah.lang.ast.If;
import mirah.lang.ast.ImplicitNil;
import mirah.lang.ast.ImplicitSelf;
import mirah.lang.ast.Import;
import mirah.lang.ast.Include;
import mirah.lang.ast.InterfaceDeclaration;
import mirah.lang.ast.LocalAccess;
import mirah.lang.ast.LocalAssignment;
import mirah.lang.ast.LocalDeclaration;
import mirah.lang.ast.Loop;
import mirah.lang.ast.MacroDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Modifier;
import mirah.lang.ast.ModifierList;
import mirah.lang.ast.Next;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeList;
import mirah.lang.ast.NodeVisitor;
import mirah.lang.ast.Noop;
import mirah.lang.ast.Not;
import mirah.lang.ast.Null;
import mirah.lang.ast.OptionalArgument;
import mirah.lang.ast.OptionalArgumentList;
import mirah.lang.ast.Package;
import mirah.lang.ast.Raise;
import mirah.lang.ast.Redo;
import mirah.lang.ast.Regex;
import mirah.lang.ast.RequiredArgument;
import mirah.lang.ast.RequiredArgumentList;
import mirah.lang.ast.Rescue;
import mirah.lang.ast.RescueClause;
import mirah.lang.ast.RescueClauseList;
import mirah.lang.ast.RestArgument;
import mirah.lang.ast.Return;
import mirah.lang.ast.Script;
import mirah.lang.ast.Self;
import mirah.lang.ast.SimpleString;
import mirah.lang.ast.StaticMethodDefinition;
import mirah.lang.ast.StringConcat;
import mirah.lang.ast.StringEval;
import mirah.lang.ast.StringPieceList;
import mirah.lang.ast.Super;
import mirah.lang.ast.Symbol;
import mirah.lang.ast.TypeNameList;
import mirah.lang.ast.TypeRefImpl;
import mirah.lang.ast.Unquote;
import mirah.lang.ast.UnquoteAssign;
import mirah.lang.ast.VCall;
import mirah.lang.ast.WhenClause;
import mirah.lang.ast.ZSuper;

/**
 *
 * @author Markov, markovs@programpark.ru
 * @Created on 30.05.2015, 10:59
 */
public abstract class AbstractNodeVisitor implements NodeVisitor {

    @Override
    public Object visitOther(Node node, Object o) {
        return null;
    }

    @Override
    public Object visitErrorNode(ErrorNode en, Object o) {
        return null;
    }

    @Override
    public Object visitTypeRefImpl(TypeRefImpl tri, Object o) {
        return null;
    }

    @Override
    public Object visitFunctionalCall(FunctionalCall fc, Object o) {
        return null;
    }

    @Override
    public Object visitVCall(VCall vcall, Object o) {
        return null;
    }

    @Override
    public Object visitCast(Cast cast, Object o) {
        return null;
    }

    @Override
    public Object visitCall(Call call, Object o) { 
        return null;
    }

    @Override
    public Object visitColon2(Colon2 colon2, Object o) {
        return null;
    }

    @Override
    public Object visitZSuper(ZSuper zsuper, Object o) {
        return null;
    }

    @Override
    public Object visitSuper(Super s, Object o) {
        return null;
    }

    @Override
    public Object visitBlockPass(BlockPass bp, Object o) {
        return null;
    }

    @Override
    public Object visitCase(Case c, Object o) {
        return null;
    }

    @Override
    public Object visitWhenClause(WhenClause wc, Object o) {
        return null;
    }

    @Override
    public Object visitIf(If i, Object o) {
        return null;
    }

    @Override
    public Object visitLoop(Loop loop, Object o) {
        return null;
    }

    @Override
    public Object visitNot(Not not, Object o) {
        return null;
    }

    @Override
    public Object visitReturn(Return r, Object o) {
        return null;
    }

    @Override
    public Object visitBreak(Break b, Object o) {
        return null;
    }

    @Override
    public Object visitNext(Next next, Object o) {
        return null;
    }

    @Override
    public Object visitRedo(Redo redo, Object o) {
        return null;
    }

    @Override
    public Object visitRaise(Raise raise, Object o) {
        return null;
    }

    @Override
    public Object visitRescueClause(RescueClause rc, Object o) {
        return null;
    }

    @Override
    public Object visitRescue(Rescue rescue, Object o) {
        return null;
    }

    @Override
    public Object visitEnsure(Ensure ensure, Object o) {
        return null;
    }

    @Override
    public Object visitUnquote(Unquote unqt, Object o) {
        return null;
    }

    @Override
    public Object visitUnquoteAssign(UnquoteAssign ua, Object o) {
        return null;
    }

    @Override
    public Object visitMacroDefinition(MacroDefinition md, Object o) {
        return null;
    }

    @Override
    public Object visitClassDefinition(ClassDefinition cd, Object o) {
        return null;
    }

    @Override
    public Object visitInterfaceDeclaration(InterfaceDeclaration id, Object o) {
        return null;
    }

    @Override
    public Object visitClosureDefinition(ClosureDefinition cd, Object o) {
        return null;
    }

    @Override
    public Object visitFieldDeclaration(FieldDeclaration fd, Object o) {
        return null;
    }

    @Override
    public Object visitFieldAssign(FieldAssign fa, Object o) {
        return null;
    }

    @Override
    public Object visitFieldAccess(FieldAccess fa, Object o) {
        return null;
    }

    @Override
    public Object visitInclude(Include incld, Object o) {
        return null;
    }

    @Override
    public Object visitConstant(Constant cnstnt, Object o) {
        return null;
    }

    @Override
    public Object visitColon3(Colon3 colon3, Object o) {
        return null;
    }

    @Override
    public Object visitConstantAssign(ConstantAssign ca, Object o) {
        return null;
    }

    @Override
    public Object visitAttrAssign(AttrAssign aa, Object o) {
        return null;
    }

    @Override
    public Object visitElemAssign(ElemAssign ea, Object o) {
        return null;
    }

    @Override
    public Object visitHashEntryList(HashEntryList hel, Object o) {
        return null;
    }

    @Override
    public Object visitNodeList(NodeList nl, Object o) {
        return null;
    }

    @Override
    public Object visitTypeNameList(TypeNameList tnl, Object o) {
        return null;
    }

    @Override
    public Object visitAnnotationList(AnnotationList al, Object o) {
        return null;
    }

    @Override
    public Object visitRescueClauseList(RescueClauseList rcl, Object o) {
        return null;
    }

    @Override
    public Object visitStringPieceList(StringPieceList spl, Object o) {
        return null;
    }

    @Override
    public Object visitRequiredArgumentList(RequiredArgumentList ral, Object o) {
        return null;
    }

    @Override
    public Object visitOptionalArgumentList(OptionalArgumentList oal, Object o) {
        return null;
    }

    @Override
    public Object visitModifierList(ModifierList ml, Object o) {
        return null;
    }

    @Override
    public Object visitArray(Array array, Object o) {
        return null;
    }

    @Override
    public Object visitFixnum(Fixnum fixnum, Object o) {
        return null;
    }

    @Override
    public Object visitFloat(Float f, Object o) {
        return null;
    }

    @Override
    public Object visitCharLiteral(CharLiteral cl, Object o) {
        return null;
    }

    @Override
    public Object visitHash(Hash hash, Object o) {
        return null;
    }

    @Override
    public Object visitHashEntry(HashEntry he, Object o) {
        return null;
    }

    @Override
    public Object visitSimpleString(SimpleString ss, Object o) {
        return null;
    }

    @Override
    public Object visitStringConcat(StringConcat sc, Object o) {
        return null;
    }

    @Override
    public Object visitStringEval(StringEval se, Object o) {
        return null;
    }

    @Override
    public Object visitRegex(Regex regex, Object o) {
        return null;
    }

    @Override
    public Object visitSymbol(Symbol symbol, Object o) {
        return null;
    }

    @Override
    public Object visitBoolean(Boolean bln, Object o) {
        return null;
    }

    @Override
    public Object visitNull(Null n, Object o) {
        return null;
    }

    @Override
    public Object visitImplicitNil(ImplicitNil in, Object o) {
        return null;
    }

    @Override
    public Object visitSelf(Self self, Object o) {
        return null;
    }

    @Override
    public Object visitImplicitSelf(ImplicitSelf is, Object o) {
        return null;
    }

    @Override
    public Object visitLocalDeclaration(LocalDeclaration ld, Object o) {
        return null;
    }

    @Override
    public Object visitLocalAssignment(LocalAssignment la, Object o) {
        return null;
    }

    @Override
    public Object visitLocalAccess(LocalAccess la, Object o) {
        return null;
    }

    @Override
    public Object visitArguments(Arguments a, Object o) {
        return null;
    }

    @Override
    public Object visitRequiredArgument(RequiredArgument ra, Object o) {
        return null;
    }

    @Override
    public Object visitOptionalArgument(OptionalArgument oa, Object o) {
        return null;
    }

    @Override
    public Object visitRestArgument(RestArgument ra, Object o) {
        return null;
    }

    @Override
    public Object visitBlockArgument(BlockArgument ba, Object o) {
        return null;
    }

    @Override
    public Object visitMethodDefinition(MethodDefinition md, Object o) {
        return null;
    }

    @Override
    public Object visitStaticMethodDefinition(StaticMethodDefinition smd, Object o) {
        return null;
    }

    @Override
    public Object visitConstructorDefinition(ConstructorDefinition cd, Object o) {
        return null;
    }

    @Override
    public Object visitClassAppendSelf(ClassAppendSelf cas, Object o) {
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object o) {
        return null;
    }

    @Override
    public Object visitBindingReference(BindingReference br, Object o) {
        return null;
    }

    @Override
    public Object visitNoop(Noop noop, Object o) {
        return null;
    }

    @Override
    public Object visitScript(Script script, Object o) {
        return null;
    }

    @Override
    public Object visitAnnotation(Annotation antn, Object o) {
        return null;
    }

    @Override
    public Object visitModifier(Modifier mdfr, Object o) {
        return null;
    }

    @Override
    public Object visitImport(Import i, Object o) {
        return null;
    }

    @Override
    public Object visitPackage(Package pckg, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyArray(EmptyArray ea, Object o) {
        return null;
    }

    @Override
    public Object visitBuiltin(Builtin bltn, Object o) {
        return null;
    }

}
