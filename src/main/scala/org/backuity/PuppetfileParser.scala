package org.backuity

import org.parboiled2.{ParseError, CharPredicate, Parser, ParserInput}

import scala.util.{Success, Failure}

class PuppetfileParser(val input: ParserInput) extends Parser {

  def ModulesRule = rule { push(Map.empty[String,String]) ~ zeroOrMore(ModuleRule) ~ zeroOrMore(WhiteSpaceChar) ~ EOI }

  def ModuleRule  = rule {
    WhiteSpace ~ ws("mod") ~ Quote ~ capture(ModuleIdentifier) ~ Quote ~ ws(',') ~
      ws(":git") ~ ws("=>") ~ Quote ~ capture(GitUri) ~ Quote ~ 
      optional( WhiteSpace ~ ws(',') ~ ws(":ref") ~ ws("=>") ~ Quote ~ capture(GitTag) ~ Quote)  ~>
      {(m: Map[String,String], moduleId: String, gitRepo : String, optRef: Option[String]) =>
        if( m.contains(moduleId) ) sys.error(s"Duplicated module $moduleId, cannot replace ${m(moduleId)} with $gitRepo" )
        m + (moduleId -> gitRepo) }
  }

  def ModuleIdentifier = rule { oneOrMore(CharPredicate.AlphaNum ++ Seq('_')) } // '-' isn't supported as it leads to invalid module names
  def GitUri = rule { oneOrMore(NonQuoteChar) }
  def GitTag = rule { oneOrMore(NonQuoteChar)}

  def ws(c: Char) = rule { c ~ WhiteSpace }
  def ws(c: String) = rule { c ~ WhiteSpace }
  def WhiteSpace = rule { zeroOrMore(WhiteSpaceChar) }

  val NonQuoteChar = CharPredicate.Visible -- Seq('"', '\'')
  val WhiteSpaceChar = CharPredicate(" \n\r\t\f")
  val Quote = CharPredicate("'\"")
}

object PuppetfileParser {

  def parse(input: String) : Map[String,String] = {
    val parser = new PuppetfileParser(input)

    parser.ModulesRule.run() match {
      case Failure(err : ParseError) =>
        sys.error(parser.formatError(err))
      case Failure(err) => throw err
      case Success(m) => m
    }
  }
}
