# Show PlayStore unsupported languages
# See: https://support.google.com/googleplay/android-developer/answer/9844778?hl=en#zippy=%2Cview-list-of-available-languages
ls -a fastlane/metadata/android/ | sort | grep --invert-match 'af\|sq\|am\|ar\|hy-AM\|az-AZ\|bn-BD\|eu-ES\|be\|bg\|my-MM\|ca\|zh-HK\|zh-CN\|zh-TW\|hr\|cs-CZ\|da-DK\|nl-NL\|en-IN\|en-SG\|en-ZA\|en-AU\|en-CA\|en-GB\|en-US\|et\|fil\|fi-FI\|fr-CA\|fr-FR\|gl-ES\|ka-GE\|de-DE\|el-GR\|gu\|iw-IL\|hi-IN\|hu-HU\|is-IS\|id\|it-IT\|ja-JP\|kn-IN\|kk\|km-KH\|ko-KR\|ky-KG\|lo-LA\|lv\|lt\|mk-MK\|ms\|ms-MY\|ml-IN\|mr-IN\|mn-MN\|ne-NP\|no-NO\|fa\|fa-AE\|fa-AF\|fa-IR\|pl-PL\|pt-BR\|pt-PT\|pa\|ro\|rm\|ru-RU\|sr\|si-LK\|sk\|sl\|es-419\|es-ES\|es-US\|sw\|sv-SE\|ta-IN\|te-IN\|th\|tr-TR\|uk\|ur\|vi\|zu'

# Show PlayStore incomplete language
find fastlane/metadata/android/ -mindepth 1 -maxdepth 1 -type d '!' -exec test -e "{}/title.txt" ';' -print
find fastlane/metadata/android/ -mindepth 1 -maxdepth 1 -type d '!' -exec test -e "{}/short_description.txt" ';' -print
find fastlane/metadata/android/ -mindepth 1 -maxdepth 1 -type d '!' -exec test -e "{}/full_description.txt" ';' -print