/*
 * Copyright 2019-2119 gao_xianglong@sina.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tools.lex01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 词法分析器
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/4/27 6:30 下午
 */
public class Lexer {
    /**
     * 符号表
     */
    private SymbolTable symbolTable;
    /**
     * 所有保留字符号串的总长度,当词素的符号串长度>此值时，意味着这是一个标识符
     */
    private int maxLength;
    /**
     * 源文件内容
     */
    private char[] codes;
    /**
     * 当前字符
     */
    private char ch;
    /**
     * 词素缓存
     */
    private char[] sbuf;
    /**
     * 文件索引
     */
    private int index;
    /**
     * 结束符号
     */
    private final byte EOI = 0x1A;
    /**
     * 换行符
     */
    private final byte LF = 0xA;
    /**
     * 回车符
     */
    private final byte CR = 0xD;
    /**
     * 反向索引表,Attribute.flag对应TokenKin枚举ID
     */
    private Map<Integer, Integer> indexs;
    private static Logger log = LoggerFactory.getLogger(Lexer.class);

    private Lexer(String str) {
        Objects.requireNonNull(str);
        codes = str.toCharArray();
        codes = Arrays.copyOf(codes, codes.length + 1);
        codes[codes.length - 1] = EOI;
        symbolTable = new SymbolTable();
        indexs = new ConcurrentHashMap<>();
    }

    /**
     * 读取下一个Token
     *
     * @return
     */
    private Token nextToken() {
        Token result = null;
        loop:
        do {
            nextChar();
            //@formatter:off
            switch (ch) {
                case ' ': case '\t':
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z':
                case '$': case '_':
                    result = scanIdent();
                    break loop;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    result = scanNumber();
                    break loop;
                //@formatter:on
                case ';':
                    try {
                        addMorpheme();
                        result = getToken();
                    } finally {
                        sbuf = null;
                    }
                    break loop;
                case '\"':
                    result = scanString();
                    break loop;
                default:
                    var token = scanOperator();
                    if (Objects.nonNull(token)) {
                        result = token;
                    }
                    break loop;
            }
        } while (ch != EOI);//当读入到结束符号时退出
        return result;
    }

    /**
     * 开始词法解析
     *
     * @return
     */
    private void parse() {
        while (true) {
            var token = nextToken();
            if (Objects.isNull(token)) break;
            log.info("{}", token);
        }
    }

    /**
     * 根据flag获取TokenKin
     *
     * @param flag
     * @return
     */
    private TokenKind getTokenKin(int flag) {
        return flag <= maxLength ? TokenKind.values()[indexs.get(flag)] : TokenKind.IDENTIFIER;
    }

    /**
     * 字符串读取
     *
     * @return
     */
    private Token scanString() {
        do {
            addMorpheme();
            nextChar();
        } while (ch != '\"' && ch != CR && ch != LF && ch != EOI);
        if (ch != '\"') throw new RuntimeException(String.format("词法解析错误:%s", new String(sbuf)));
        addMorpheme();
        try {
            return getToken(TokenKind.STRINGLITERAL);
        } finally {
            sbuf = null;
        }
    }

    /**
     * 读取一个完整的特殊符号Token
     */
    private Token scanOperator() {
        if (ch == EOI) return null;
        while (true) {
            switch (ch) {
                //@formatter:off
                case '!': case '%': case '&': case '*':
                case '?': case '+': case '-': case ':':
                case '<': case '=': case '>': case '^':
                case '|': case '~': case '@': case '/':
                    break;
                //@formatter:on
                default:
                    try {
                        return getToken();
                    } finally {
                        prevChar();
                        sbuf = null;
                    }
            }
            addMorpheme();
            var attribute = symbolTable.getAttribute(sbuf);
            var tokenKind = getTokenKin(attribute.flag);
            if (TokenKind.IDENTIFIER == tokenKind) {
                try {
                    sbuf = Arrays.copyOf(sbuf, sbuf.length - 1);
                    return getToken();
                } finally {
                    prevChar();
                    sbuf = null;
                }
            }
            nextChar();
        }
    }

    /**
     * 读取一个完整的数字字面值Token
     */
    private Token scanNumber() {
        while (true) {
            switch (ch) {
                //@formatter:off
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                default:
                   try{
                       return getToken(TokenKind.INTLITERAL);
                   }finally{
                       prevChar();
                       sbuf = null;
                   }
                //@formatter:on
            }
            addMorpheme();
            nextChar();
        }
    }

    /**
     * 读取一个完成的标识符Token
     *
     * @return
     */
    private Token scanIdent() {
        while (true) {
            //@formatter:off
            switch (ch) {
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z':
                case '$': case '_':
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                //@formatter:on
                default:
                    try {
                        return getToken();
                    } finally {
                        prevChar();
                        sbuf = null;
                    }
            }
            addMorpheme();
            nextChar();
        }
    }

    /**
     * 组装词素
     */
    private void addMorpheme() {
        sbuf = Objects.isNull(sbuf) ? new char[1] : Arrays.copyOf(sbuf, sbuf.length + 1);
        sbuf[sbuf.length - 1] = ch;
    }

    /**
     * 封装返回Token
     *
     * @return
     */
    private Token getToken(TokenKind tokenKin) {
        var attribute = symbolTable.getAttribute(sbuf);//从符号表中获取出属性对象
        //根据属性对象的flag字段从反向索引表中获取出TokenKin的序数，再根据序数获取出对应的TokenKin
        return new Token(attribute, Objects.isNull(tokenKin) ?
                getTokenKin(attribute.flag) : tokenKin);
    }

    private Token getToken() {
        return getToken(null);
    }

    /**
     * 移动索引，读取下一个符号
     */
    private void nextChar() {
        if (index < codes.length) {
            ch = codes[index++];
        }
    }

    /**
     * 移动索引，读取上一个符号
     */
    private void prevChar() {
        ch = codes[--index];
    }

    /**
     * 相关初始化操作
     *
     * @return
     */
    private Lexer init() {
        TokenKind[] tokenKins = TokenKind.values();
        Stream.of(tokenKins).forEach(tokenKind -> {
            var name = tokenKind.name;
            if (Objects.nonNull(name)) {
                var attribute = symbolTable.getAttribute(name.toCharArray());//根据词素从符号表中获取出对应的属性对象，如果不存在就先添加
                var flag = attribute.flag;
                indexs.put(flag, tokenKind.ordinal());
                maxLength = maxLength < flag ? flag : maxLength;
            }
        });
        log.info("maxLength:{}", maxLength);
        return this;
    }

    /**
     * 符号表
     */
    static class SymbolTable {
        /**
         * 记录所有保留字的符号串长度
         */
        private int length;
        private Map<Chars, Attribute> attributes;

        private SymbolTable() {
            attributes = new ConcurrentHashMap<>();
        }

        /**
         * 根据词素从符号表中获取出对应的属性对象，如果不存在就先添加
         *
         * @param morpheme
         * @return
         */
        private Attribute getAttribute(char[] morpheme) {
            var chars = new Chars(morpheme);
            var attribute = attributes.get(chars);
            if (Objects.isNull(attribute)) {
                length += morpheme.length;
                attribute = new Attribute(morpheme, length);
                attributes.put(chars, attribute);
            }
            return attribute;
        }
    }

    /**
     * 词法单元,即Token序列
     */
    static class Token {
        /**
         * Token对应的相关属性
         */
        private Attribute attribute;
        /**
         * Token类型
         */
        private TokenKind tokenKind;

        private Token(Attribute attribute, TokenKind tokenKind) {
            this.attribute = attribute;
            this.tokenKind = tokenKind;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "attribute:" + attribute +
                    ", tokenKind:" + tokenKind +
                    '}';
        }
    }

    /**
     * 目前仅支持一些二元表达式
     */
    enum TokenKind {
        //@formatter:off
        IDENTIFIER, INT("int"), EQ("="), INTLITERAL("0-9"),
        SEMI(";"), LT("<"), GT(">"), EQEQ("=="),
        LTEQ("<="), GTEQ(">="), PLUS("+"), SUB("-"),
        STAR("*"), SLASH("/"), PLUSEQ("+="), SUBEQ("-="),
        STAREQ("*="), SLASHEQ("/="),STRINGLITERAL("string"),
        TRUE("true"), FALSE("false"), BOOLEAN("boolean");
        //@formatter:on
        private String name;

        TokenKind() {
        }

        TokenKind(String name) {
            this.name = name;
        }
    }

    static class Attribute {
        /**
         * 词素
         */
        private char[] morpheme;
        /**
         * 保留字标记
         */
        private int flag;

        private Attribute(char[] morpheme, int flag) {
            this.morpheme = morpheme;
            this.flag = flag;
        }

        @Override
        public String toString() {
            return String.format("morpheme:'%s',flag:%s", new String(morpheme), flag);
        }
    }

    /**
     * 对词素进行封装
     */
    static class Chars {
        private char[] morpheme;

        private Chars(char[] morpheme) {
            this.morpheme = morpheme;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Chars)) return false;
            Chars chars = (Chars) o;
            return Arrays.equals(morpheme, chars.morpheme);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(morpheme);
        }
    }

    public static void main(String[] agrs) {
        new Lexer("String str = \"Hello World\";" +
                "int v1 = 100;" +
                "boolean v2 = true").init().parse();
    }
}