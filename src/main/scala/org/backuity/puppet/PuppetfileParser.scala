package org.backuity.puppet

import org.parboiled2.{ParseError, CharPredicate, Parser, ParserInput}

import scala.util.{Success, Failure}

sealed abstract class Module

case class GitModule(gitUri: String, ref: Option[String] = None) extends Module
case class ForgeModule(version: String) extends Module

case class Puppetfile(forge: Option[String] = None,
                      modules : Map[String, Module] = Map.empty)

class PuppetfileParser(val input: ParserInput) extends Parser {

  def ModulesRule = rule { optional(Forge) ~ push(Map.empty[String,Module]) ~ zeroOrMore(ModuleRule) ~ Blanks ~ EOI ~>
      { (forge: Option[String], modules: Map[String,Module]) =>
        Puppetfile(forge, modules)
      }
  }

  def Forge = rule { Blanks ~ ws("forge") ~ Quote ~ capture(oneOrMore(NonQuoteChar)) ~ Quote }

  def ModuleRule = rule {
    Blanks ~ ws("mod") ~ Quote ~ capture(ModuleIdentifier) ~ Quote ~ ws(',') ~
        (GitModuleRule | ForgeModuleRule) ~>
      {(m: Map[String,Module], moduleId: String, module: Module) =>
         if( m.contains(moduleId) ) sys.error(s"Duplicated module $moduleId, cannot replace ${m(moduleId)} with $module" )
         m + (moduleId -> module) }
  }

  // we might want to have stricter module identifier for GitModule?
  def ModuleIdentifier = rule { oneOrMore(CharPredicate.AlphaNum ++ Seq('_','/')) } // '-' isn't supported as it leads to invalid module names

  def GitModuleRule = rule {
    ws(":git") ~ ws("=>") ~ Quote ~ capture(GitUri) ~ Quote ~
        optional( Blanks ~ ws(',') ~ ws(":ref") ~ ws("=>") ~ Quote ~ capture(GitTag) ~ Quote) ~>
        {(gitUri: String, ref: Option[String]) => GitModule(gitUri, ref)}
  }
  def GitUri = rule { oneOrMore(NonQuoteChar) }
  def GitTag = rule { oneOrMore(NonQuoteChar)}

  def ForgeModuleRule = rule {
    Quote ~ capture(ModuleVersion) ~ Quote ~> ForgeModule
  }
  def ModuleVersion = rule { oneOrMore(NonQuoteChar) }

  def ws(c: Char) = rule { c ~ Blanks }
  def ws(c: String) = rule { c ~ Blanks }
  def Blanks = rule { zeroOrMore(BlankChar) }

  val NonQuoteChar = CharPredicate.Visible -- Seq('"', '\'')
  val BlankChar = CharPredicate(" \n\r\t\f")
  val Quote = CharPredicate("'\"")
}

object PuppetfileParser {

  def parse(input: String) : Puppetfile = {
    val parser = new PuppetfileParser(input)

    parser.ModulesRule.run() match {
      case Failure(err : ParseError) =>
        sys.error(parser.formatError(err))
      case Failure(err) => throw err
      case Success(m) => m
    }
  }
}