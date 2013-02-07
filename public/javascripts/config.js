var config = {
  debug: false,
  title: 'Twitter wall built by @rem',

  search: 'from:@joyofcoding OR #joc2013 OR joyofcoding.org OR #joyofcoding OR #joyofcoding2013',

  timings: {
    defaultNoticeHoldTime: '10s',
    showTweetsEvery: '3s'
  }
};

// allows reuse in the node script
if (typeof exports !== 'undefined') {
  module.exports = config;
} 
