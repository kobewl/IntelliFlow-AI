var gulp         = require('gulp');
var less         = require('gulp-less');
var sourcemaps   = require('gulp-sourcemaps');
var autoprefix   = require('gulp-autoprefixer');
var minify       = require('gulp-minify-css');
var rename       = require('gulp-rename');
var header       = require('gulp-header');
var nunjucks     = require('gulp-nunjucks-render');
var pkgFile      = require('./package.json');
var banner       = ['/** <%= package.name %> | <%= package.homepage %> */ '];

gulp.task('styles', function() {
  return gulp.src('src/less/*.less')
    .pipe(less())
    .pipe(autoprefix({browsers:['last 2 versions']}))
    .pipe(minify())
    .pipe(header(banner, {package: pkgFile}))
    .pipe(rename({
      suffix:  '.min',
      extname: '.css'
    }))
    .pipe(gulp.dest('dist/')) // <-- deliver compressed for distribution
    .pipe(gulp.dest('docs/')) // <-- deliver copy for docs
})

gulp.task('docs', function() {
  return gulp.src('src/docs/pages/*.njk')
    .pipe(nunjucks({
      path: 'src/docs/partials/',
      data: { package: pkgFile }
    }))
    .pipe(gulp.dest('docs/'));
})

gulp.task('default', ['styles', 'docs'], function() {
  gulp.watch('src/less/**/*', ['styles']),
  gulp.watch('src/docs/**/*', ['docs'])
})
