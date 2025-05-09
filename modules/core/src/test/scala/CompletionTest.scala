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

package net.andimiller.decline.completion

import cats.implicits._
import com.indoorvivants.snapshots.munit_integration._
import com.monovore.decline.completion.Folder
import com.monovore.decline.completion.Folder.CompleteableCommand
import com.monovore.decline.{Command, Opts}

class CompletionSpec extends munit.FunSuite with MunitSnapshotsIntegration {
  test("Folder should fold a cli into a model we can use") {
    val cli = Command("foo", "the foo command") {
      (
        Opts.argument[String]("file.txt"),
        Opts.flag("verbose", "enable verbose logs").orNone.map(_.isDefined)
      ).mapN { case (f, v) =>
        123
      }
    }
    assertEquals(
      Folder.buildModel(cli),
      CompleteableCommand(
        "foo",
        "the foo command",
        List(
          "--help" -> "Print help and usage information.",
          "--verbose" -> "enable verbose logs"
        ),
        List("file.txt")
      )
    )
  }
  test("Completion should be able to produce zsh and bash scripts") {
    val deeper = Command("binary", "help") {
      val number = Opts.argument[Int]("n")
      val verbose = Opts.flag("verbose", "enable verbose debug logs").orFalse
      val extraFlag = Opts.flag(
        "extra-flag",
        "extra flag only on add, with a short form",
        "e"
      )
      val add = Opts.subcommand("add", "add two numbers") {
        (number, number, verbose, extraFlag).mapN { case (a, b, v, _) =>
          if (v) println(s"adding $a and $b")
          println(a + b)
        }
      }
      val multiply = Opts.subcommand("multiply", "multiply two numbers") {
        (number, number, verbose).mapN { case (a, b, v) =>
          if (v) println(s"multiplying $a and $b")
          println(a * b)
        }
      }
      add orElse multiply
    }
    assertSnapshot("bash.completion", Completion.bashCompletion(deeper))
    assertSnapshot("zsh.completion", Completion.zshBashcompatCompletion(deeper))
  }
}
