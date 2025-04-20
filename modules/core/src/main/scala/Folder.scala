/*
 * Copyright 2023 Andi Miller and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.monovore.decline.completion

import cats.implicits._
import com.monovore.decline.Command
import com.monovore.decline.Opt
import com.monovore.decline.Opts
import com.monovore.decline.Opts.Name

object Folder {

  case class CompleteableCommand(
      name: String,
      help: String,
      flags: List[(String, String)] = Nil,
      arguments: List[String] = Nil,
      subcommands: List[CompleteableCommand] = Nil
  ) {

    private def completionTree(
        path: List[String]
    )(c: CompleteableCommand): Map[List[String], List[(String, String)]] = {
      val here = c.name :: path
      val completions = c.flags ++ c.subcommands.map(s => s.name -> s.help)
      c.subcommands
        .map { s =>
          completionTree(here)(s)
        }
        .fold(Map(here -> completions)) { case (m, n) => m ++ n }
    }

    def completionTree(): Map[List[String], List[(String, String)]] =
      completionTree(List.empty)(this)
  }

  type Modifier = CompleteableCommand => CompleteableCommand

  def nameToString(n: Name): String = n match {
    case Opts.LongName(flag)  => "--" + flag
    case Opts.ShortName(flag) => "-" + flag
  }

  def one(p: Opt[_]): Modifier = p match {
    case Opt.Regular(names, metavar, help, visibility) =>
      c => c.copy(flags = c.flags ++ names.map(nameToString).tupleRight(help))
    case Opt.OptionalOptArg(names, _, _, _) =>
      c => c.copy(arguments = c.arguments ++ names.map(nameToString))
    case Opt.Flag(names, help, visibility) =>
      c => c.copy(flags = c.flags ++ names.map(nameToString).tupleRight(help))
    case Opt.Argument(metavar) =>
      c => c.copy(arguments = c.arguments ++ List(metavar))
  }

  def many(p: Opts[_]): Modifier = p match {
    case Opts.Pure(_) => identity
    case Opts.HelpFlag(_) =>
      c =>
        c.copy(flags =
          ("--help", "Print help and usage information.") +: c.flags
        )
    case Opts.App(f, a) =>
      many(f) andThen many(a)
    case Opts.OrElse(a, b) =>
      many(a) andThen many(b)
    case Opts.Single(opt) =>
      one(opt)
    case Opts.Repeated(opt) =>
      one(opt)
    case Opts.Validate(a, _) =>
      many(a)
    case Opts.Subcommand(s) =>
      c => c.copy(subcommands = c.subcommands.prepended(buildModel(s)))
    case _: Opts.Env => identity
    case _           => identity
  }

  def buildModel(c: Command[_]): CompleteableCommand =
    many(c.options)(CompleteableCommand(c.name, c.header))

}
