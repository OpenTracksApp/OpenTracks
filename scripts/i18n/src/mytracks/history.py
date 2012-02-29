'''
Module which brings history information about files from Mercurial.

@author: Rodrigo Damazio
'''

import re
import subprocess

REVISION_REGEX = re.compile(r'(?P<hash>[0-9a-f]{12}):.*')

def _GetOutputLines(args):
  '''
  Runs an external process and returns its output as a list of lines.

  @param args: the arguments to run
  '''
  process = subprocess.Popen(args,
                             stdout=subprocess.PIPE,
                             universal_newlines = True,
                             shell = False)
  output = process.communicate()[0]
  return output.splitlines()


def FillMercurialRevisions(filename, parsed_file):
  '''
  Fills the revs attribute of all strings in the given parsed file with
  a list of revisions that touched the lines corresponding to that string.
  
  @param filename: the name of the file to get history for
  @param parsed_file: the parsed file to modify
  '''
  # Take output of hg annotate to get revision of each line
  output_lines = _GetOutputLines(['hg', 'annotate', '-c', filename])

  # Create a map of line -> revision (key is list index, line 0 doesn't exist)
  line_revs = ['dummy']
  for line in output_lines:
    rev_match = REVISION_REGEX.match(line)
    if not rev_match:
      raise 'Unexpected line of output from hg: %s' % line
    rev_hash = rev_match.group('hash')
    line_revs.append(rev_hash)

  for str in parsed_file.itervalues():
    # Get the lines that correspond to each string
    start_line = str['startLine']
    end_line = str['endLine']

    # Get the revisions that touched those lines
    revs = []
    for line_number in range(start_line, end_line + 1):
      revs.append(line_revs[line_number])

    # Merge with any revisions that were already there
    # (for explict revision specification)
    if 'revs' in str:
      revs += str['revs']

    # Assign the revisions to the string
    str['revs'] = frozenset(revs)

def DoesRevisionSuperceed(filename, rev1, rev2):
  '''
  Tells whether a revision superceeds another.
  This essentially means that the older revision is an ancestor of the newer
  one.
  This also returns True if the two revisions are the same.

  @param rev1: the revision that may be superceeding the other
  @param rev2: the revision that may be superceeded
  @return: True if rev1 superceeds rev2 or they're the same
  '''
  if rev1 == rev2:
    return True

  # TODO: Add filename
  args = ['hg', 'log', '-r', 'ancestors(%s)' % rev1, '--template', '{node|short}\n', filename]
  output_lines = _GetOutputLines(args)

  return rev2 in output_lines

def NewestRevision(filename, rev1, rev2):
  '''
  Returns which of two revisions is closest to the head of the repository.
  If none of them is the ancestor of the other, then we return either one.

  @param rev1: the first revision
  @param rev2: the second revision
  '''
  if DoesRevisionSuperceed(filename, rev1, rev2):
    return rev1
  return rev2