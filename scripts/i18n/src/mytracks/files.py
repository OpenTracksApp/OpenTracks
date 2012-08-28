'''
Module for dealing with resource files (but not their contents).

@author: Rodrigo Damazio
'''
import os.path
from glob import glob
import re

MYTRACKS_RES_DIR = 'MyTracks/res'
ANDROID_MASTER_VALUES = 'values'
ANDROID_VALUES_MASK = 'values-*'


def GetMyTracksDir():
  '''
  Returns the directory in which the MyTracks directory is located.
  '''
  path = os.getcwd()
  while not os.path.isdir(os.path.join(path, MYTRACKS_RES_DIR)):
    if path == '/':
      raise 'Not in My Tracks project'

    # Go up one level
    path = os.path.split(path)[0]

  return path


def GetAllLanguageFiles():
  '''
  Returns a mapping from all found languages to their respective directories.
  '''
  mytracks_path = GetMyTracksDir()
  res_dir = os.path.join(mytracks_path, MYTRACKS_RES_DIR, ANDROID_VALUES_MASK)
  language_dirs = glob(res_dir)
  master_dir = os.path.join(mytracks_path, MYTRACKS_RES_DIR, ANDROID_MASTER_VALUES)
  if len(language_dirs) == 0:
    raise 'No languages found!'
  if not os.path.isdir(master_dir):
    raise 'Couldn\'t find master file'

  language_tuples = [(re.findall(r'.*values-([A-Za-z-]+)', dir)[0],dir) for dir in language_dirs]
  language_tuples.append(('en', master_dir))
  return dict(language_tuples)
