module Jekyll

  #
  # Render gist
  #
  # Usage:
  #
  # {% gist gist_id %} =>
  #
  # Raises: Liquid::SyntaxError
  class Gist < ::Liquid::Tag

    Syntax = /(#{::Liquid::Expression}+)?/

    def initialize(tag_name, markup, tokens)
      if markup =~ Syntax
        @gist_id = $1
        @options = {}
        markup.scan(::Liquid::TagAttributes) { |key, value| @options[key.to_sym] = value.gsub(/"|'/, '') }
      else
        raise ::Liquid::SyntaxError.new("Syntax Error in 'link' - Valid syntax: gist <id> <options>")
      end

      super
    end

    def render(context)
      %{<script src="http://gist.github.com/#{@gist_id}.js"></script>}
    end
  end

end


Liquid::Template.register_tag('gist', Jekyll::Gist)
