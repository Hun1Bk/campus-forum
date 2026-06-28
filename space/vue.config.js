const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    historyApiFallback: true,
  },
  configureWebpack: {
    resolve: {
      extensions: [".ts", ".tsx", ".js", ".json"],
      alias: {}
    },
    module: {
      rules: [
        {
          test: /\.tsx?$/,
          loader: 'ts-loader',
          exclude: /node_modules/,
          options: {
            appendTsSuffixTo: [/\.vue$/],
          }
        }
      ]
    }
  }
})
