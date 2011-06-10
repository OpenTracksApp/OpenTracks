'''
Module which prompts the user for translations and saves them.

TODO: implement

@author: Rodrigo Damazio
'''

class Translator(object):
  '''
  classdocs
  '''

  def __init__(self, language):
    '''
    Constructor
    '''
    self._language = language

  def Translate(self, string_names):
    print string_names