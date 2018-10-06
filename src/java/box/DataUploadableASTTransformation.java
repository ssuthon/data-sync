package box;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import java.lang.reflect.Modifier;
import java.util.List;
import static org.codehaus.groovy.ast.MethodNode.ACC_PUBLIC;
import static org.codehaus.groovy.ast.MethodNode.ACC_STATIC;


@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class DataUploadableASTTransformation implements ASTTransformation {
	
	public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

		for (ASTNode astNode : astNodes) {
			if (astNode instanceof ClassNode) {
				ClassNode classNode = (ClassNode) astNode;				
				createAdditionalField(classNode);
			}
		}
	}

	public void createAdditionalField(ClassNode classNode){				
		classNode.addProperty("syncUuid", Modifier.PUBLIC, new ClassNode(String.class), null, null, null);
		addSettings("constraints", classNode, "syncUuid", "nullable: true, bindable: false");

		String idx_name = classNode.getNameWithoutPackage() + "_SYNCUUID_IDX";
		addSettings("mapping", classNode, "syncUuid", "index: '" + idx_name + "'");
	}

	public void addSettings(String name,ClassNode classNode,String fieldName,String config){
		if(config==null) return;
		
		String configStr = fieldName + " " + config;
		
		BlockStatement newConfig = (BlockStatement) new AstBuilder().buildFromString(configStr).get(0); 

		FieldNode closure = classNode.getField(name);
		if(closure == null){
			createStaticClosure(classNode, name);
			closure = classNode.getField(name);
			assert closure != null;
		} 
		
		if(!hasFieldInClosure(closure,fieldName)){			
			ReturnStatement returnStatement = (ReturnStatement) newConfig.getStatements().get(0);
			ExpressionStatement exStatment = new ExpressionStatement(returnStatement.getExpression());
			ClosureExpression exp = (ClosureExpression)closure.getInitialExpression();
			BlockStatement block = (BlockStatement) exp.getCode();
			block.addStatement(exStatment);
		}

		assert hasFieldInClosure(closure,fieldName) == true;
	}

	public boolean hasFieldInClosure(FieldNode closure, String fieldName){
		if(closure != null){
			ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
			BlockStatement block = (BlockStatement) exp.getCode();
			List<Statement> ments = block.getStatements();
			for(Statement expstat : ments){
				if(expstat instanceof ExpressionStatement && ((ExpressionStatement)expstat).getExpression() instanceof MethodCallExpression){
					MethodCallExpression methexp = (MethodCallExpression)((ExpressionStatement)expstat).getExpression();
					ConstantExpression conexp = (ConstantExpression)methexp.getMethod();
					if(conexp.getValue().equals(fieldName)){
						return true;
					}
				}
			}
		}
		return false;
	}

	public void createStaticClosure(ClassNode classNode,String name){
		FieldNode field = new FieldNode(name, ACC_PUBLIC | ACC_STATIC, new ClassNode(java.lang.Object.class), new ClassNode(classNode.getClass()),null);
		ClosureExpression expr = new ClosureExpression(Parameter.EMPTY_ARRAY, new BlockStatement());
		expr.setVariableScope(new VariableScope());
		field.setInitialValueExpression(expr);
		classNode.addField(field);
	}
}