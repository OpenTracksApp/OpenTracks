#!/usr/bin/python
'''
Entry point for My Tracks i18n tool.

@author: Rodrigo Damazio
'''

import mytracks.files
import mytracks.translate
import mytracks.validate
import sys

def Usage():
  print 'Usage: %s <command> [<language> ...]\n' % sys.argv[0]
  print 'Commands are:'
  print '  cleanup'
  print '  translate'
  print '  validate'
  sys.exit(1)


def Translate(languages):
  '''
  Asks the user to interactively translate any missing or oudated strings from
  the files for the given languages.

  @param languages: the languages to translate
  '''
  validator = mytracks.validate.Validator(languages)
  validator.Validate()
  missing = validator.missing_in_lang()
  outdated = validator.outdated_in_lang()

  for lang in languages:
    untranslated = missing[lang] + outdated[lang]
    
    if len(untranslated) == 0:
      continue

    translator = mytracks.translate.Translator(lang)
    translator.Translate(untranslated)


def Validate(languages):
  '''
  Computes and displays errors in the string files for the given languages.

  @param languages: the languages to compute for
  '''
  validator = mytracks.validate.Validator(languages)
  validator.Validate()

  error_count = 0
  if (validator.valid()):
    print 'All files OK'
  else:
    for lang, missing in validator.missing_in_master().iteritems():
      print 'Missing in master, present in %s: %s:' % (lang, str(missing))
      error_count = error_count + len(missing)
    for lang, missing in validator.missing_in_lang().iteritems():
      print 'Missing in %s, present in master: %s:' % (lang, str(missing))
      error_count = error_count + len(missing)
    for lang, outdated in validator.outdated_in_lang().iteritems():
      print 'Outdated in %s: %s:' % (lang, str(outdated))
      error_count = error_count + len(outdated)

  return error_count


if __name__ == '__main__':
  argv = sys.argv
  argc = len(argv)
  if argc < 2:
    Usage()

  languages = mytracks.files.GetAllLanguageFiles()
  if argc == 3:
    langs = set(argv[2:])
    if not langs.issubset(languages):
      raise 'Language(s) not found'

    # Filter just to the languages specified
    languages = dict((lang, lang_file)
                     for lang, lang_file in languages.iteritems()
                     if lang in langs or lang == 'en' )

  cmd = argv[1]
  if cmd == 'translate':
    Translate(languages)
  elif cmd == 'validate':
    error_count = Validate(languages)
  else:
    Usage()
    error_count = 0

  print '%d errors found.' % error_count
