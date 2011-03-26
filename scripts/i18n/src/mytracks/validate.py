'''
Module which compares languague files to the master file and detects
issues.

@author: Rodrigo Damazio
'''

import os
from mytracks.parser import StringsParser
import mytracks.history 

class Validator(object):

  def __init__(self, languages):
    '''
    Builds a strings file validator.
    
    Params:
    @param languages: a dictionary mapping each language to its corresponding directory
    '''
    self._langs = {}
    self._master = None
    self._language_paths = languages

    parser = StringsParser()
    for lang, lang_dir in languages.iteritems():
      filename = os.path.join(lang_dir, 'strings.xml')
      parsed_file = parser.Parse(filename)
      mytracks.history.FillMercurialRevisions(filename, parsed_file)

      if lang == 'en':
        self._master = parsed_file
      else:
        self._langs[lang] = parsed_file

    self._Reset()

  def Validate(self):
    '''
    Computes whether all the data in the files for the given languages is valid.
    '''
    self._Reset()
    self._ValidateMissingKeys()
    self._ValidateOutdatedKeys()

  def valid(self):
    return (len(self._missing_in_master) == 0 and
            len(self._missing_in_lang) == 0 and
            len(self._outdated_in_lang) == 0)

  def missing_in_master(self):
    return self._missing_in_master

  def missing_in_lang(self):
    return self._missing_in_lang

  def outdated_in_lang(self):
    return self._outdated_in_lang

  def _Reset(self):
    # These are maps from language to string name list
    self._missing_in_master = {}
    self._missing_in_lang = {}
    self._outdated_in_lang = {}

  def _ValidateMissingKeys(self):
    '''
    Computes whether there are missing keys on either side.
    '''
    master_keys = frozenset(self._master.iterkeys())
    for lang, file in self._langs.iteritems():
      keys = frozenset(file.iterkeys())
      missing_in_master = keys - master_keys
      missing_in_lang = master_keys - keys

      if len(missing_in_master) > 0:
        self._missing_in_master[lang] = missing_in_master
      if len(missing_in_lang) > 0:
        self._missing_in_lang[lang] = missing_in_lang

  def _ValidateOutdatedKeys(self):
    '''
    Computers whether any of the language keys are outdated with relation to the
    master keys.
    '''
    for lang, file in self._langs.iteritems():
      outdated = []
      for key, str in file.iteritems():
        # Get all revisions that touched master and language files for this
        # string.
        master_str = self._master[key]
        master_revs = master_str['revs']
        lang_revs = str['revs']
        if not master_revs or not lang_revs:
          print 'WARNING: No revision for %s in %s' % (key, lang)
          continue

        master_file = os.path.join(self._language_paths['en'], 'strings.xml')
        lang_file = os.path.join(self._language_paths[lang], 'strings.xml')

        # Assume that the repository has a single head (TODO: check that),
        # and as such there is always one revision which superceeds all others.
        master_rev = reduce(
            lambda r1, r2: mytracks.history.NewestRevision(master_file, r1, r2),
            master_revs)
        lang_rev = reduce(
            lambda r1, r2: mytracks.history.NewestRevision(lang_file, r1, r2),
            lang_revs)

        # If the master version is newer than the lang version
        if mytracks.history.DoesRevisionSuperceed(lang_file, master_rev, lang_rev):
          outdated.append(key)

      if len(outdated) > 0:
        self._outdated_in_lang[lang] = outdated
