package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Opcodes;

import java.awt.*;

public class EqualityExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public EqualityExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    public boolean isEqual() {
        return operator.getType().equals(TokenType.EQUAL_EQUAL);
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
    }

    @Override
    public CatscriptType getType() {
        return CatscriptType.BOOLEAN;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        if (isEqual()) {
            return leftHandSide.evaluate(runtime) == rightHandSide.evaluate(runtime);
        } else {
            return leftHandSide.evaluate(runtime) != rightHandSide.evaluate(runtime);
        }

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        getLeftHandSide().compile(code);
        box(code, getLeftHandSide().getType());
        getRightHandSide().compile(code);
        box(code, getRightHandSide().getType());
        org.objectweb.asm.Label equalLabel = new org.objectweb.asm.Label();
        org.objectweb.asm.Label endLabel = new org.objectweb.asm.Label();
        if (isEqual()){
            code.addJumpInstruction(Opcodes.IF_ACMPEQ, equalLabel);
            code.pushConstantOntoStack(0);
            code.addJumpInstruction(Opcodes.GOTO, endLabel);
            code.addLabel(equalLabel);
            code.pushConstantOntoStack(1);
            code.addLabel(endLabel);
        } else {
            code.addJumpInstruction(Opcodes.IF_ACMPNE, equalLabel);
            code.pushConstantOntoStack(0);
            code.addJumpInstruction(Opcodes.GOTO, endLabel);
            code.addLabel(equalLabel);
            code.pushConstantOntoStack(1);
            code.addLabel(endLabel);
        }
    }


}
