require 'liquid'

module Helpers
  #
  # Render file & gist example
  #
  # Usage:
  #
  # {% example "file_to_expand", gist_id %} =>
  #
  # Raises: Liquid::SyntaxError
  class Example < ::Liquid::Tag

    Syntax = /(#{::Liquid::Expression}+), (#{::Liquid::Expression}+)?/

    def initialize(tag_name, markup, tokens)
      if markup =~ Syntax
        @name = $1
        @gist_id = $2
        @options = {}
        markup.scan(::Liquid::TagAttributes) { |key, value| @options[key.to_sym] = value.gsub(/"|'/, '') }
      else
        raise ::Liquid::SyntaxError.new("Syntax Error in 'link' - Valid syntax: gist <id> <options>")
      end

      super
    end

    def render(context)
      %{<pre>#{File.open("./content/articles/examples/#{@name}", "rb").read}</pre><span class="help-block">(if the example above isn't displayed, see this "gist":https://gist.github.com/#{@gist_id})</span>}
    end
  end

  class YardLink < ::Liquid::Tag

    Syntax = /(#{::Liquid::Expression}+)?/

    def initialize(tag_name, markup, tokens)
      if markup =~ Syntax
        @class_name = $1
        @options = {}
        markup.scan(::Liquid::TagAttributes) { |key, value| @options[key.to_sym] = value.gsub(/"|'/, '') }
      else
        raise ::Liquid::SyntaxError.new("Syntax Error in 'link' - Valid syntax: gist <id> <options>")
      end

      super
    end

    def calculate_path full_name
      path = full_name.gsub("::", "/").gsub(".", "#")
      unless is_class?(full_name)
        if is_instance_method?(full_name)
          path += "-instance_method"
        else
          path += "-class_method"
        end
      end
      path
    end

    def is_instance_method? full_name
      !(full_name =~ /\#/).nil?
    end

    def is_class? full_name
      (full_name =~ /\./ || full_name =~ /\#/).nil?
    end

    def render(context)
      %{<a class="highlight" href="http://rubydoc.info/github/ruby-amqp/amqp/master/#{calculate_path(@class_name)}">#{@class_name}</a>}
    end
  end

end

Liquid::Template.register_tag('example', Helpers::Example)
Liquid::Template.register_tag('yard_link', Helpers::YardLink)

class LiquidFilter < Nanoc3::Filter
  identifier :liquid

  type :text

  def run(content, params={})
    Liquid::Template.parse(content).render
  rescue Exception => e
    puts "================================================================================================"
    puts "Error Message: #{e}"
    puts "================================================================================================"
    puts "Content (only beginning): \n #{content[0..1000]}"
    puts "================================================================================================"
  end
end
