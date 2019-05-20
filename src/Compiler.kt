class Compiler(private val _class: Class) {
    val className = _class.name
    private val table = SymbolTable(_class)
    private var subroutineTable: Map<String, SymbolValue>? = null

    fun compileClass() {
        _class.subroutineDec.forEach { compileSubroutine(it) }
    }

    private fun compileSubroutine(subroutineDec: SubroutineDec) {
        subroutineTable = table.subroutineTableCreator(subroutineDec)
        println("function $className.${subroutineDec.name} ${subroutineDec.paramList.list.count()}")
        compileStatements(subroutineDec.body.statements)
        val type = subroutineDec.type
        if (type is VoidOrType.Void) {
            println("push constant 0")
        }
        println("return")
    }

    private fun compileStatements(statements: Statements) {
        statements.statements.forEach {
            when (it) {
                is Stmt.Do -> {
                    compileDoStatement(it.stmt)
                }
                is Stmt.Let -> {
                    compileLetStatement(it.stmt)
                }
            }
        }
    }

    private fun compileLetStatement(letStatement: LetStatement) {
        val table = subroutineTable ?: throw Error("let: subroutineTableがnull")
        val symbolInfo = table[letStatement.varName.name] ?: throw Error("subroutineテーブルに無い")
        val index = symbolInfo.index
        val exp = letStatement.exp
        compileExpression(exp)
        if (symbolInfo.attribute == Attribute.Argument) {
            println("pop argument $index")
        } else if (symbolInfo.attribute == Attribute.Var) {
            println("pop local $index")
        }
    }

    private fun compileDoStatement(doStatement: DoStatement) {
        val classOrVarName = doStatement.subroutineCall.classOrVarName
        val subroutineName = doStatement.subroutineCall.subroutineName
        val expList = doStatement.subroutineCall.expList.expList
        if (classOrVarName != null) {
            expList.forEach { compileExpression(it) }
            println("call ${classOrVarName.name}.${subroutineName.name} ${expList.count()}")
        } else {
            expList.forEach { compileExpression(it) }
            println("call ${subroutineName.name} ${expList.count()}")
        }
        println("pop temp 0")
    }

    private fun compileExpression(exp: Expression) {
        val first = exp.expElms.first()
        if (exp.expElms.count() > 1) {
            val op = exp.expElms[1]
            val rest = exp.expElms.slice(2 until exp.expElms.count())
            if (first is ExpElm._Term && op is ExpElm._Op) {
                compileTerm(first.term)
                compileExpression(Expression(rest))
                compileOperand(op.op)
            }
        } else if (first is ExpElm._Term) {
            compileTerm(first.term)
        }
    }

    private fun compileTerm(term: Term) {
        if (term is Term.IntC) {
            println("push const ${term.const}")
        } else if (term is Term.VarName) {
            val table = subroutineTable ?: throw Error("let: subroutineTableがnull")
            val symbolInfo = table[term.name] ?: throw Error("subroutineテーブルに無い")
            if (symbolInfo.attribute == Attribute.Argument) {
                println("push argument ${symbolInfo.index}")
            } else if (symbolInfo.attribute == Attribute.Var) {
                println("push local ${symbolInfo.index}")
            }
        } else if (term is Term._Expression) {
            compileExpression(term.exp)
        }
    }

    private fun compileOperand(op: Op) {
        if (op == Op.Plus) {
            println("add")
        } else if (op == Op.Minus) {
            println("sub")
        } else if (op == Op.Asterisk) {
            println("call Math.multiply 2")
        } else if (op == Op.Slash) {
            println("call Math.divide 2")
        }
    }
}