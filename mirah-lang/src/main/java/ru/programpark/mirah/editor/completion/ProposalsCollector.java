package ru.programpark.mirah.editor.completion;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;

/**
 * ProposalsCollector is responsible for collecting code completion proposals.
 * 
 * Provides various method for different types of completion (completeMethods,
 * completeTypes, completeFields etc.).
 *
 * @author Martin Janicek
 */
public class ProposalsCollector {

    private List<CompletionProposal> proposals;
    private CompletionContext context;

    private BaseCompletion typesCompletion;
    private BaseCompletion fieldCompletion;
    private BaseCompletion methodCompletion;
    private BaseCompletion newVarCompletion;
    private BaseCompletion keywordCompletion;
    private BaseCompletion packageCompletion;
    private BaseCompletion localVarCompletion;
    private BaseCompletion camelCaseCompletion;
    private BaseCompletion namedParamCompletion;


    public ProposalsCollector(CompletionContext context) {
        this.context = context;
        
        proposals = new ArrayList<CompletionProposal>();
        typesCompletion = new TypesCompletion();
        fieldCompletion = new FieldCompletion();
        methodCompletion = new MethodCompletion();
        newVarCompletion = new NewVarCompletion();
        keywordCompletion = new KeywordCompletion();
        packageCompletion = new PackageCompletion();
        localVarCompletion = new LocalVarCompletion();
        camelCaseCompletion = new ConstructorGenerationCompletion();
        namedParamCompletion = new NamedParamsCompletion();
    }

    public void completeKeywords(CompletionContext completionRequest) {
        keywordCompletion.complete(proposals, completionRequest, context.getAnchor());
    }

    public void completeMethods(CompletionContext completionRequest) {
        methodCompletion.complete(proposals, completionRequest, context.getAnchor());
    }

    public void completeFields(CompletionContext completionRequest) {
        fieldCompletion.complete(proposals, completionRequest, context.getAnchor());
    }

    public void completeCamelCase(CompletionContext request) {
        camelCaseCompletion.complete(proposals, request, context.getAnchor());
    }

    public void completeTypes(CompletionContext request) {
        typesCompletion.complete(proposals, request, context.getAnchor());
    }

    public void completePackages(CompletionContext request) {
        packageCompletion.complete(proposals, request, context.getAnchor());
    }

    public void completeLocalVars(CompletionContext request) {
        localVarCompletion.complete(proposals, request, context.getAnchor());
    }

    public void completeNewVars(CompletionContext request) {
        newVarCompletion.complete(proposals, request, context.getAnchor());
    }

    public void completeNamedParams(CompletionContext request) {
        namedParamCompletion.complete(proposals, request, context.getAnchor());
    }

    public List<CompletionProposal> getCollectedProposals() {
        return proposals;
    }

    public void clearProposals() {
        proposals.clear();
    }
}
