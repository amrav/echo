#include <bits/stdc++.h>
using namespace std;

typedef long long int ll;

int main() {

  cout << "Enter url: ";
  string url;
  cin >> url;
  int i, temp;
  ll final_no = 0;
  
  for(i = 0; i < (int) url.length(); i++) {
    if(url[i] >= 'A' & url[i] <= 'z') temp = url[i] - 'A';
    else temp = 52 + url[i] - '0';
    temp += 1;
    final_no *= 63;
    final_no += temp;
    
  }
  cout << final_no << endl;
  return 0;
}
  
