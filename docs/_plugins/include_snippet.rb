# Title: Include Snippet Tag for Jekyll
# Author: Eitan Yarden https://github.com/eitan101
# Description: Import your code snippets into any blog post.
# Configuration: You can set default import path in _config.yml (defaults to code_dir: downloads/code)
#
# Syntax {% include_snippet snippet_name path/to/file %}
#
# Example 1:
# ~~~ java
# {% include_code hello world javascripts/test.java %}
# ~~~
#
# This will import test.js starting from the line which contains "snippet hello world" till
# the line which contains "end of snippet". You can use the "snippet_exclude_begin" and "snippet_exclude_end" for
# block exclusion. For code higlight wrap the tag with the appropritate md tag.
#
#

require 'pathname'

module Jekyll

  class IncludeCodeTag < Liquid::Tag
    # include HighlightCode
    # include TemplateWrapper
    def initialize(tag_name, markup, tokens)
      @title = nil
      if markup.strip =~ /(.*)?(\s+|^)(\/*\S+)/i
        @title = $1 || nil
        @file = $3
      end
      super
    end

    def render(context)
      code_dir = (context.registers[:site].config['code_dir'].sub(/^\//,'') || 'downloads/code')
      code_path = (Pathname.new(context.registers[:site].source) + code_dir).expand_path
      file = code_path + @file

      if File.symlink?(code_path)
        return "Code directory '#{code_path}' cannot be a symlink"
      end

      unless file.file?
        return "File #{file} could not be found"
      end

      Dir.chdir(code_path) do
        code = file.read
        source = ""
        inblock = false;
        exclude = false;
        code.lines.each_with_index do |line,index|
          if line =~ /end of snippet/
            inblock = false
          end
          if (line =~ /snippet_exclude_begin/)
            exclude = true
          end
          if inblock && !exclude
              source  += "#{line}"
          end
          if (line =~ /snippet_exclude_end/)
            exclude = false
          end
          if line =~ /snippet #{@title}/
            inblock = true
          end
        end
        ouput = source
      end
    end
  end

end

Liquid::Template.register_tag('include_snippet', Jekyll::IncludeCodeTag)
