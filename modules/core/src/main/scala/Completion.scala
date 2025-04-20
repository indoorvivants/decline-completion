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
import com.monovore.decline._
import com.monovore.decline.completion.Folder

object Completion {

  def bashCompletion(c: Command[_]): String = {
    val model = Folder.buildModel(c)
    val conditions = model.completionTree().toList.sortBy(-_._1.size).map {
      case (path, wordsWithHelp) =>
        val (words, _) = wordsWithHelp.separate
        val cond = path.reverse.zipWithIndex
          .map { case (cmd, idx) =>
            s"\"$${cmds[$idx]}\" = \"$cmd\""
          }
          .mkString("[[ ", " && ", " ]]")
        s"""if $cond; then
         |  COMPREPLY=( $$(compgen -W '${words.mkString(" ")}' -- "$$cur"))
         |  return
         |fi
         |""".stripMargin
    }

    s"""
       |_${c.name}()
       |{
       |local cmds=($${COMP_WORDS[@]//-*}) # Filter out any flags or options
       |local cur
       |if [[ "$$SHELL" =~ ".*zsh" ]]; then
       |  cur=$$COMP_CWORD
       |else
       |  cur=`_get_cword`
       |fi
       |${conditions.mkString("\n")}
       |}
       |complete -F _${c.name} ${c.name}
       |""".stripMargin
  }

  def zshBashcompatCompletion(c: Command[_]): String =
    """autoload bashcompinit
      |bashcompinit
      |""".stripMargin + bashCompletion(c)

}
