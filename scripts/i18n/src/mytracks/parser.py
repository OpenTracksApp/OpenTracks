'''
Module which parses a string XML file.

@author: Rodrigo Damazio
'''

from xml.parsers.expat import ParserCreate
import re
#import xml.etree.ElementTree as ET

class StringsParser(object):
  '''
  Parser for string XML files.

  This object is not thread-safe and should be used for parsing a single file at
  a time, only.
  '''

  def Parse(self, file):
    '''
    Parses the given file and returns a dictionary mapping keys to an object
    with attributes for that key, such as the value, start/end line and explicit
    revisions.
    
    In addition to the standard XML format of the strings file, this parser
    supports an annotation inside comments, in one of these formats:

    <!-- KEEP_PARENT name="bla" -->
    <!-- KEEP_PARENT name="bla" rev="123456789012" -->

    Such an annotation indicates that we're explicitly inheriting form the
    master file (and the optional revision says that this decision is compatible
    with the master file up to that revision).

    @param file: the name of the file to parse
    '''
    self._Reset()

    # Unfortunately expat is the only parser that will give us line numbers
    self._xml_parser = ParserCreate()
    self._xml_parser.StartElementHandler = self._StartElementHandler
    self._xml_parser.EndElementHandler = self._EndElementHandler
    self._xml_parser.CharacterDataHandler = self._CharacterDataHandler
    self._xml_parser.CommentHandler = self._CommentHandler

    file_obj = open(file)
    self._xml_parser.ParseFile(file_obj)
    file_obj.close()

    return self._all_strings

  def _Reset(self):
    self._currentString = None
    self._currentStringName = None
    self._currentStringValue = None
    self._all_strings = {}

  def _StartElementHandler(self, name, attrs):
    if name != 'string':
      return

    if 'name' not in attrs:
      return

    assert not self._currentString
    assert not self._currentStringName
    self._currentString = {
        'startLine' : self._xml_parser.CurrentLineNumber,
    }

    if 'rev' in attrs:
      self._currentString['revs'] = [attrs['rev']]

    self._currentStringName = attrs['name']
    self._currentStringValue = ''

  def _EndElementHandler(self, name):
    if name != 'string':
      return

    assert self._currentString
    assert self._currentStringName
    self._currentString['value'] = self._currentStringValue
    self._currentString['endLine'] = self._xml_parser.CurrentLineNumber
    self._all_strings[self._currentStringName] = self._currentString

    self._currentString = None
    self._currentStringName = None
    self._currentStringValue = None

  def _CharacterDataHandler(self, data):
    if not self._currentString:
      return

    self._currentStringValue += data

  _KEEP_PARENT_REGEX = re.compile(r'\s*KEEP_PARENT\s+'
                                  r'name\s*=\s*[\'"]?(?P<name>[a-z0-9_]+)[\'"]?'
                                  r'(?:\s+rev=[\'"]?(?P<rev>[0-9a-f]{12})[\'"]?)?\s*',
                                  re.MULTILINE | re.DOTALL)

  def _CommentHandler(self, data):
    keep_parent_match = self._KEEP_PARENT_REGEX.match(data)
    if not keep_parent_match:
      return

    name = keep_parent_match.group('name')
    self._all_strings[name] = {
        'keepParent' : True,
        'startLine' : self._xml_parser.CurrentLineNumber,
        'endLine' : self._xml_parser.CurrentLineNumber
    }
    rev = keep_parent_match.group('rev')
    if rev:
      self._all_strings[name]['revs'] = [rev]