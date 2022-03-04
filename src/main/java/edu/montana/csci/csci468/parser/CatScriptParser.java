package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;
import java.util.LinkedList;
import java.util.List;


import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        Statement asssingmentStatment = parseAssignmentStatement();
        if (asssingmentStatment != null){
            return asssingmentStatment;
        }
        Statement ifStatement = parseIfStatemnet();
        if (ifStatement != null){
            return ifStatement;
        }
        Statement forStatement = parseForStatement();
        if (forStatement != null) {
            return forStatement;
        }
        Statement variableStatement = parseVariableStatement();
        if (variableStatement != null) {
            return variableStatement;
        }
        Statement funcDecStatement = parseFunctionDecStatement();
        if (funcDecStatement != null) {
            return funcDecStatement;
        }

        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseFunctionDecStatement() {
        FunctionDefinitionStatement funcStatement = new FunctionDefinitionStatement();
        if(tokens.getCurrentToken().getStringValue().equals("function")) {
            funcStatement.setStart(tokens.consumeToken());
            Token name = tokens.getCurrentToken();
            require(IDENTIFIER, funcStatement);
            funcStatement.setName(name.getStringValue());
            require(LEFT_PAREN, funcStatement);
            parseParameterList(funcStatement);
            require(RIGHT_PAREN, funcStatement);
            require(LEFT_BRACE, funcStatement);
            List<Statement> stateList = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE)) {
                if (tokens.getCurrentToken().getStringValue().equals("return")) {
                    tokens.consumeToken();
                    ReturnStatement returState = new ReturnStatement();
                    returState.setExpression(parseExpression());
                    stateList.add(returState);
                }
                Statement state = parseProgramStatement();
                stateList.add(state);
            }
            funcStatement.setBody(stateList);
            funcStatement.setEnd(tokens.getCurrentToken());
            return funcStatement;
        } else {
            return null;
        }

    }

    private void parseParameterList(FunctionDefinitionStatement funcState){
        TypeLiteral type = new TypeLiteral();
        while (!tokens.match(RIGHT_PAREN)) {
            Token name = tokens.consumeToken();
            if (tokens.getCurrentToken().getStringValue().equals(":")) {
                tokens.consumeToken();
                type.setType(parseTypeExpression());
            } else {
                type.setType(null);
            }
            if(tokens.match(COMMA)) {
                tokens.consumeToken();
            }
            funcState.addParameter(name.getStringValue(), type);
        }
    }

    private Statement parseVariableStatement() {
        VariableStatement varStatement = new VariableStatement();
        if (tokens.getCurrentToken().getStringValue().equals("var")) {
            varStatement.setStart(tokens.consumeToken());
            if (tokens.match(IDENTIFIER)) {
                varStatement.setVariableName(tokens.getCurrentToken().getStringValue());
                tokens.consumeToken();
                if (tokens.getCurrentToken().getStringValue().equals(":")) {
                    tokens.consumeToken();
                    CatscriptType type = parseTypeExpression();
                    varStatement.setExplicitType(type);
                    tokens.consumeToken();
                }
                require(EQUAL, varStatement);
                varStatement.setExpression(parseExpression());
            }
            varStatement.setEnd(tokens.getCurrentToken());
            return varStatement;
        } else {
            return null;
        }
    }

    private CatscriptType parseTypeExpression(){
        if (tokens.getCurrentToken().getStringValue().equals("int")) {
            return CatscriptType.INT;
        }
        if (tokens.getCurrentToken().getStringValue().equals("bool")) {
            return CatscriptType.BOOLEAN;
        }
        if (tokens.getCurrentToken().getStringValue().equals("string")) {
            return CatscriptType.STRING;
        }
        if (tokens.getCurrentToken().getStringValue().equals("object")) {
            return CatscriptType.OBJECT;
        }
        return null;
    }

    private Statement parseForStatement(){
        ForStatement forStatement = new ForStatement();
        if (tokens.getCurrentToken().getStringValue().equals("for")){
            forStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, forStatement);
            forStatement.setVariableName(tokens.getCurrentToken().getStringValue());
            tokens.consumeToken();
            tokens.consumeToken();
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);
            require(LEFT_BRACE, forStatement);
            List<Statement> stateList = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE)) {
                if(tokens.match(EOF)) {
                    forStatement.addError(ErrorType.UNEXPECTED_TOKEN);
                    break;
                }
                Statement state = parseProgramStatement();
                stateList.add(state);
            }
            //require(RIGHT_BRACE, forStatement);
            forStatement.setBody(stateList);
        } else {
            return null;
        }
        forStatement.setEnd(require(RIGHT_BRACE, forStatement));
        return forStatement;
    }

    private Statement parseIfStatemnet() {
        IfStatement ifStatement = new IfStatement();
        if(tokens.getCurrentToken().getStringValue().equals("if")){
            ifStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);
            require(LEFT_BRACE, ifStatement);
            List<Statement> stateList = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE)) {
                if(tokens.match(EOF)) {
                    ifStatement.addError(ErrorType.UNEXPECTED_TOKEN);
                    break;
                }
                Statement state = parseProgramStatement();
                stateList.add(state);
            }
            require(RIGHT_BRACE, ifStatement);
            ifStatement.setTrueStatements(stateList);
            // need to check if there is an else statement
            if (tokens.getCurrentToken().getStringValue().equals("else")) {
                require(LEFT_BRACE, ifStatement);
                List<Statement> elseList = new LinkedList<>();
                while (!tokens.match(RIGHT_BRACE)) {
                    if(tokens.match(EOF)) {
                        ifStatement.addError(ErrorType.UNEXPECTED_TOKEN);
                        break;
                    }
                    Statement state = parseProgramStatement();
                    elseList.add(state);
                }
                //require(RIGHT_BRACE, ifStatement);
                ifStatement.setElseStatements(elseList);
            }
        } else {
            return null;
        }
        ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));
        return ifStatement;
    }

    private Statement parseAssignmentStatement(){
        if(tokens.match(IDENTIFIER)){

            AssignmentStatement assignmentStatement = new AssignmentStatement();
            assignmentStatement.setVariableName(tokens.getCurrentToken().getStringValue());
            assignmentStatement.setStart(tokens.consumeToken());
            require(EQUAL, assignmentStatement);
            assignmentStatement.setExpression(parseExpression());
            assignmentStatement.setEnd(tokens.getCurrentToken());
            return assignmentStatement;

        } else {
            return null;
        }
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseCompassionExpression();
        while (tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseCompassionExpression();
            EqualityExpression equalExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalExpression.setStart(expression.getStart());
            equalExpression.setEnd(rightHandSide.getEnd());
            expression = equalExpression;
        }
        return expression;
    }

    private Expression parseCompassionExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression compExpression = new ComparisonExpression(operator, expression, rightHandSide);
            compExpression.setStart(expression.getStart());
            compExpression.setEnd(rightHandSide.getEnd());
            expression = compExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(IDENTIFIER)) {

            if (tokens.getNext().getType().equals(LEFT_PAREN)){
                return parseFunctionExpression();
            }
            // do another if that checks the next token and calls functions if it does
            Token idenToken = tokens.consumeToken();
            IdentifierExpression idenExpress = new IdentifierExpression(idenToken.getStringValue());
            idenExpress.setToken(idenToken);
            return idenExpress;
        } else if (tokens.match(STRING)) {
            Token strToken = tokens.consumeToken();
            StringLiteralExpression strExpress = new StringLiteralExpression(strToken.getStringValue());
            strExpress.setToken(strToken);
            return strExpress;
        } else if (tokens.match(TRUE)) {
            tokens.consumeToken();
            BooleanLiteralExpression trueExp = new BooleanLiteralExpression(true);
            return trueExp;
        } else if (tokens.match(FALSE)) {
            tokens.consumeToken();
            BooleanLiteralExpression falseExp = new BooleanLiteralExpression(false);
            return falseExp;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExp = new NullLiteralExpression();
            return nullExp;

        } else if (tokens.match(LEFT_PAREN)) {
            tokens.consumeToken();
            ParenthesizedExpression expression = new ParenthesizedExpression(parseExpression());
            tokens.consumeToken();
            return expression;
        } else if(tokens.match(LEFT_BRACKET)) {
            return parseListExpression();
        }
        else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private Expression parseFunctionExpression(){
        Token token = tokens.consumeToken();
        ErrorType er = null;
        tokens.consumeToken();
        List<Expression> expressionList = new LinkedList<>();
        while(!tokens.match(RIGHT_PAREN)){
            if (tokens.match(EOF)){
                er = ErrorType.UNTERMINATED_ARG_LIST;
                break;
            }
            Expression expression = parseExpression();
            expressionList.add(expression);
            if (tokens.match(COMMA)) {
                tokens.consumeToken();
            }
        }
        FunctionCallExpression fucntionExpression = new FunctionCallExpression(token.getStringValue(), expressionList);
        if(er != null) {
            fucntionExpression.addError(er);
        }
        return fucntionExpression;
    }

    private Expression parseListExpression(){
        Token token = tokens.consumeToken();
        ErrorType er = null;
        List<Expression> expressList = new LinkedList<>();
        while(!tokens.match(RIGHT_BRACKET)) {
            if (tokens.match(EOF)){
                er = ErrorType.UNTERMINATED_LIST;
                break;
            }
            Expression expression = parseExpression();
            expressList.add(expression);
            if (tokens.match(COMMA)) {
                tokens.consumeToken();
            }
        }
        tokens.matchAndConsume(RIGHT_BRACKET);
        ListLiteralExpression listExpress = new ListLiteralExpression(expressList);
        if(er != null) {
            listExpress.addError(er);
        }
        return listExpress;
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
