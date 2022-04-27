package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ForStatement extends Statement {
    private Expression expression;
    private String variableName;
    private List<Statement> body;

    public void setExpression(Expression expression) {
        this.expression = addChild(expression);
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setBody(List<Statement> statements) {
        this.body = new LinkedList<>();
        for (Statement statement : statements) {
            this.body.add(addChild(statement));
        }
    }

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public List<Statement> getBody() {
        return body;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        symbolTable.pushScope();
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            expression.validate(symbolTable);
            CatscriptType type = expression.getType();
            if (type instanceof CatscriptType.ListType) {
                symbolTable.registerSymbol(variableName, getComponentType());
            } else {
                addError(ErrorType.INCOMPATIBLE_TYPES, getStart());
                symbolTable.registerSymbol(variableName, CatscriptType.OBJECT);
            }
        }
        for (Statement statement : body) {
            statement.validate(symbolTable);
        }
        symbolTable.popScope();
    }

    private CatscriptType getComponentType() {
        return ((CatscriptType.ListType) expression.getType()).getComponentType();
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        Iterable obj = (Iterable) expression.evaluate(runtime);
        for (Object o : obj) {
            runtime.pushScope();
            runtime.setValue(variableName, o);
            for (Statement statement : body) {
                statement.execute(runtime);
            }
            runtime.popScope();
        }

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {

        Integer iterSlot = code.nextLocalStorageSlot();
        Label iterStart = new Label();
        Label end = new Label();

        expression.compile(code);
        code.addMethodInstruction(Opcodes.INVOKEINTERFACE, ByteCodeGenerator.internalNameFor(List.class), "iterator", "()java/until/Iterator");
        code.addVarInstruction(Opcodes.ASTORE, iterSlot);
        code.addLabel(iterStart);
        code.addVarInstruction(Opcodes.ALOAD, iterSlot);
        code.addMethodInstruction(Opcodes.INVOKEINTERFACE, ByteCodeGenerator.internalNameFor(Iterator.class), "hasNext", "()Z");
        code.addJumpInstruction(Opcodes.IFEQ, end);

        CatscriptType componentType = getComponentType();
        code.addVarInstruction(Opcodes.ALOAD, iterSlot);
        code.addMethodInstruction(Opcodes.INVOKEINTERFACE, ByteCodeGenerator.internalNameFor(Iterator.class), "next", "()Ljava/lang/Object;");
        code.addTypeInstruction(Opcodes.CHECKCAST, ByteCodeGenerator.internalNameFor(componentType.getJavaType()));
        unbox(code, componentType);

        Integer localStorageSlot = code.createLocalStorageSlotFor(variableName);
        // store the value into the slot

        // compile the body
        // jump back up
        code.addLabel(end);
    }

}
